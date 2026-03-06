import defaultProfile from '@/assets/default_profile.png';
import useBinaryContentStore, { BinaryContentInfo } from '@/stores/binaryContentStore';
import useMessageStore from '@/stores/messageStore';
import useUserStore from '@/stores/userStore';
import { BinaryContentDto, ChannelDto } from '@/types/api';
import { useEffect, useState } from 'react';
import InfiniteScroll from 'react-infinite-scroll-component';
import { Avatar } from '../../styles/common';
import {
  ActionButton,
  AttachmentList,
  AuthorAvatarContainer,
  CancelButton,
  EditActions,
  EditButton,
  EditInput,
  FileIcon,
  FileInfo,
  FileItem,
  FileName,
  FileSize,
  ImagePreview,
  MessageActions,
  MessageAuthor,
  MessageContent,
  MessageHeader,
  MessageItem,
  MessageListWrapper,
  MessageTime,
  StyledMessageList
} from './styles';

interface MessageListProps {
  channel: ChannelDto;
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B';
  else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  else if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  else return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
};

function MessageList({ channel }: MessageListProps): JSX.Element {
  const { messages, loadMoreMessages, pagination, startPolling, stopPolling, updateMessage, deleteMessage } = useMessageStore();
  const { binaryContents, fetchBinaryContent } = useBinaryContentStore();
  const currentUserId = useUserStore((state) => state.currentUserId);

  const [hoveredMessageId, setHoveredMessageId] = useState<string | null>(null);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState('');

  useEffect(() => {
    if (channel?.id) {
      startPolling(channel.id);

      return () => {
        stopPolling(channel.id);
      };
    }
  }, [channel?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    messages.forEach(message => {
      message.attachments?.forEach(attachment => {
        if (!binaryContents[attachment.id]) {
          fetchBinaryContent(attachment.id);
        }
      });
    });
  }, [messages, binaryContents, fetchBinaryContent]);

  const handleEditStart = (messageId: string, content: string) => {
    setEditingMessageId(messageId);
    setEditContent(content);
  };

  const handleEditSubmit = async (messageId: string) => {
    if (!editContent.trim()) return;
    await updateMessage(messageId, { newContent: editContent.trim() });
    setEditingMessageId(null);
    setEditContent('');
  };

  const handleEditCancel = () => {
    setEditingMessageId(null);
    setEditContent('');
  };

  const handleDelete = async (messageId: string) => {
    await deleteMessage(messageId);
  };

  const handleDownload = async (attachment: BinaryContentInfo) => {
    try {
      const { url, fileName } = attachment;
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;

      link.style.display = 'none';
      document.body.appendChild(link);

      try {
        const handle = await (window as any).showSaveFilePicker({
          suggestedName: attachment.fileName,
          types: [{
            description: 'Files',
            accept: {
              '*/*': ['.txt', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.jpg', '.jpeg', '.png', '.gif']
            }
          }]
        });

        const writable = await handle.createWritable();
        const response = await fetch(url);
        const blob = await response.blob();
        await writable.write(blob);
        await writable.close();
      } catch (err: any) {
        if (err.name !== 'AbortError') {
          link.click();
        }
      }

      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('파일 다운로드 실패:', error);
    }
  };

  const renderAttachments = (attachments?: BinaryContentDto[]) => {
    if (!attachments?.length) return null;

    return attachments.map((_attachment) => {
      const attachment = binaryContents[_attachment.id];
      if (!attachment) return null;

      const isImage = attachment.contentType.startsWith('image/');

      if (isImage) {
        return (
          <AttachmentList key={attachment.url}>
            <ImagePreview
              href="#"
              onClick={(e) => {
                e.preventDefault();
                handleDownload(attachment);
              }}
            >
              <img
                src={attachment.url}
                alt={attachment.fileName}
              />
            </ImagePreview>
          </AttachmentList>
        );
      }

      return (
        <AttachmentList key={attachment.url}>
          <FileItem
            href="#"
            onClick={(e) => {
              e.preventDefault();
              handleDownload(attachment);
            }}
          >
            <FileIcon>
              <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                <path d="M8 3C8 1.89543 8.89543 1 10 1H22L32 11V37C32 38.1046 31.1046 39 30 39H10C8.89543 39 8 38.1046 8 37V3Z" fill="#0B93F6" fillOpacity="0.1"/>
                <path d="M22 1L32 11H24C22.8954 11 22 10.1046 22 9V1Z" fill="#0B93F6" fillOpacity="0.3"/>
                <path d="M13 19H27M13 25H27M13 31H27" stroke="#0B93F6" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </FileIcon>
            <FileInfo>
              <FileName>{attachment.fileName}</FileName>
              <FileSize>{formatFileSize(attachment.size)}</FileSize>
            </FileInfo>
          </FileItem>
        </AttachmentList>
      );
    });
  };

  const formatTime = (dateTimeString: string) => {
    return new Date(dateTimeString).toLocaleTimeString();
  };

  const fetchMoreData = () => {
    if (channel?.id) {
      loadMoreMessages(channel.id);
    }
  };

  return (
    <MessageListWrapper>
      <div id="scrollableDiv" style={{ height: '100%', overflow: 'auto', display: 'flex', flexDirection: 'column-reverse' }}>
        <InfiniteScroll
          dataLength={messages.length}
          next={fetchMoreData}
          hasMore={pagination.hasNext}
          loader={<h4 style={{ textAlign: 'center' }}>메시지를 불러오는 중...</h4>}
          scrollableTarget="scrollableDiv"
          style={{ display: 'flex', flexDirection: 'column-reverse' }}
          inverse={true}
          endMessage={
            <p style={{ textAlign: 'center' }}>
              <b>{!pagination.hasNext && messages.length > 0 ? "모든 메시지를 불러왔습니다" : ""}</b>
            </p>
          }
        >
          <StyledMessageList>
            {[...messages].reverse().map(message => {
              const author = message.author;
              const isMyMessage = currentUserId && author?.id === currentUserId;
              const isEditing = editingMessageId === message.id;
              const isHovered = hoveredMessageId === message.id;

              return (
                <MessageItem
                  key={message.id}
                  onMouseEnter={() => setHoveredMessageId(message.id)}
                  onMouseLeave={() => setHoveredMessageId(null)}
                >
                  <AuthorAvatarContainer>
                    <Avatar
                      src={author && author.profile ? binaryContents[author.profile.id]?.url : defaultProfile}
                      alt={author && author.username || '알 수 없음'}
                    />
                  </AuthorAvatarContainer>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <MessageHeader>
                      <MessageAuthor>{author && author.username || '알 수 없음'}</MessageAuthor>
                      <MessageTime>
                        {formatTime(message.createdAt)}
                      </MessageTime>
                    </MessageHeader>
                    {isEditing ? (
                      <>
                        <EditInput
                          value={editContent}
                          onChange={(e) => setEditContent(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                              e.preventDefault();
                              handleEditSubmit(message.id);
                            }
                            if (e.key === 'Escape') {
                              handleEditCancel();
                            }
                          }}
                          autoFocus
                        />
                        <EditActions>
                          <EditButton onClick={() => handleEditSubmit(message.id)}>저장</EditButton>
                          <CancelButton onClick={handleEditCancel}>취소</CancelButton>
                        </EditActions>
                      </>
                    ) : (
                      <MessageContent>{message.content}</MessageContent>
                    )}
                    {renderAttachments(message.attachments)}
                  </div>
                  {isMyMessage && isHovered && !isEditing && (
                    <MessageActions>
                      <ActionButton onClick={() => handleEditStart(message.id, message.content)}>
                        수정
                      </ActionButton>
                      <ActionButton $danger onClick={() => handleDelete(message.id)}>
                        삭제
                      </ActionButton>
                    </MessageActions>
                  )}
                </MessageItem>
              );
            })}
          </StyledMessageList>
        </InfiniteScroll>
      </div>
    </MessageListWrapper>
  );
}

export default MessageList;
