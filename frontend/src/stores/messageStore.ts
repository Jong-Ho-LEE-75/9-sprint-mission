import { create } from 'zustand';
import { getMessages, createMessage as apiCreateMessage, updateMessage as apiUpdateMessage, deleteMessage as apiDeleteMessage } from '../api/message';
import useReadStatusStore from './readStatusStore';
import { MessageDto, MessageCreateRequest, MessageUpdateRequest, Pageable } from '../types/api';

interface PollingIntervals {
  [channelId: string]: NodeJS.Timeout | boolean;
}

interface Pagination {
  nextCursor: string | null;
  pageSize: number;
  hasNext: boolean;
}

interface MessageStore {
  messages: MessageDto[];
  pollingIntervals: PollingIntervals;
  lastMessageId: string | null;
  pagination: Pagination;
  fetchMessages: (channelId: string, pageable?: Pageable, cursor?: string | null) => Promise<boolean>;
  loadMoreMessages: (channelId: string) => Promise<void>;
  startPolling: (channelId: string) => void;
  stopPolling: (channelId: string) => void;
  createMessage: (messageData: MessageCreateRequest, attachments?: File[]) => Promise<MessageDto>;
  updateMessage: (messageId: string, request: MessageUpdateRequest) => Promise<void>;
  deleteMessage: (messageId: string) => Promise<void>;
}

const defaultPageable: Pageable = {
  page: 0,
  size: 50,
  sort: ["createdAt,desc"]
};

// 전역 단조증가 카운터: 같은 채널에 startPolling이 재호출돼도 이전 doPoll 루프를 식별·종료
let _nextPollId = 0;
const activePollIds: Record<string, number> = {};

const useMessageStore = create<MessageStore>((set, get) => ({
  messages: [],
  pollingIntervals: {},
  lastMessageId: null,
  pagination: {
    nextCursor: null,
    pageSize: 50,
    hasNext: false,
  },

  fetchMessages: async (channelId, pageable = defaultPageable, cursor = null) => {
    try {
      const response = await getMessages(channelId, pageable, cursor);
      const messageList = response.content;
      const lastMessage = messageList.length > 0 ? messageList[0] : null;
      const hasNewMessages = lastMessage?.id !== get().lastMessageId;

      set((state) => {
        const isLoadMore = cursor != null;
        const isChannelChanged = channelId !== state.messages[0]?.channelId;

        let updatedMessages: MessageDto[];
        let pagination = { ...state.pagination };

        if (isLoadMore) {
          // 무한 스크롤: 커서 이전의 이전 메시지 추가
          const existingIds = new Set(state.messages.map(m => m.id));
          const olderMessages = messageList.filter(m => !existingIds.has(m.id));
          updatedMessages = [...state.messages, ...olderMessages];
          pagination = {
            nextCursor: response.nextCursor ?? null,
            pageSize: response.size,
            hasNext: response.hasNext,
          };
        } else if (state.messages.length === 0 || isChannelChanged) {
          // 최초 로딩
          updatedMessages = messageList;
          pagination = {
            nextCursor: response.nextCursor ?? null,
            pageSize: response.size,
            hasNext: response.hasNext,
          };
        } else {
          // 폴링: 현재 메시지보다 새로운 것만 추가
          const existingIds = new Set(state.messages.map(m => m.id));
          const newMessages = messageList.filter(m =>
            !existingIds.has(m.id) &&
            (state.messages.length === 0 || m.createdAt > state.messages[0].createdAt)
          );
          updatedMessages = [...newMessages, ...state.messages];
        }

        return {
          messages: updatedMessages,
          lastMessageId: lastMessage?.id || null,
          pagination,
        };
      });

      return hasNewMessages;
    } catch (error) {
      console.error('메시지 목록 조회 실패:', error);
      return false;
    }
  },

  loadMoreMessages: async (channelId) => {
    const { pagination } = get();
    if (!pagination.hasNext || !pagination.nextCursor) return;
    await get().fetchMessages(channelId, defaultPageable, pagination.nextCursor);
  },

  startPolling: (channelId) => {
    const pollId = ++_nextPollId;
    activePollIds[channelId] = pollId;

    const existing = get().pollingIntervals[channelId];
    if (existing && typeof existing !== 'boolean') {
      clearTimeout(existing as NodeJS.Timeout);
    }

    let pollInterval = 3000;
    const maxInterval = 5000;

    set((state) => ({
      pollingIntervals: { ...state.pollingIntervals, [channelId]: true }
    }));

    const doPoll = async () => {
      if (activePollIds[channelId] !== pollId) return;
      if (!get().pollingIntervals[channelId]) return;

      const hasNewMessages = await get().fetchMessages(channelId, defaultPageable);

      if (hasNewMessages) {
        pollInterval = 3000;
      } else {
        pollInterval = Math.min(pollInterval * 1.5, maxInterval);
      }

      if (activePollIds[channelId] !== pollId) return;
      if (!get().pollingIntervals[channelId]) return;

      const timeoutId = setTimeout(doPoll, pollInterval);
      set((state) => ({
        pollingIntervals: { ...state.pollingIntervals, [channelId]: timeoutId }
      }));
    };

    doPoll();
  },

  stopPolling: (channelId) => {
    activePollIds[channelId] = -1;

    const { pollingIntervals } = get();
    if (pollingIntervals[channelId]) {
      const timeoutId = pollingIntervals[channelId];
      if (typeof timeoutId !== 'boolean') {
        clearTimeout(timeoutId as NodeJS.Timeout);
      }
      set((state) => {
        const updated = { ...state.pollingIntervals };
        delete updated[channelId];
        return { pollingIntervals: updated };
      });
    }
  },

  updateMessage: async (messageId, request) => {
    const updatedMessage = await apiUpdateMessage(messageId, request);
    set((state) => ({
      messages: state.messages.map(msg => msg.id === messageId ? updatedMessage : msg)
    }));
  },

  deleteMessage: async (messageId) => {
    await apiDeleteMessage(messageId);
    set((state) => ({
      messages: state.messages.filter(msg => msg.id !== messageId)
    }));
  },

  createMessage: async (messageData, attachments) => {
    try {
      const newMessage = await apiCreateMessage(messageData, attachments);

      const updateReadStatus = useReadStatusStore.getState().updateReadStatus;
      await updateReadStatus(messageData.channelId);

      set((state) => {
        const messageExists = state.messages.some(msg => msg.id === newMessage.id);
        if (messageExists) {
          return state;
        }
        return {
          messages: [newMessage, ...state.messages],
          lastMessageId: newMessage.id
        };
      });
      return newMessage;
    } catch (error) {
      console.error('메시지 생성 실패:', error);
      throw error;
    }
  }
}));

export default useMessageStore;
