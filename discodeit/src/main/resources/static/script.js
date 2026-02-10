// API endpoints
const ENDPOINTS = {
    USERS: '/users'
};

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    fetchAndRenderUsers();
});

// Fetch users from the API
async function fetchAndRenderUsers() {
    try {
        const response = await fetch(ENDPOINTS.USERS);
        if (!response.ok) throw new Error('Failed to fetch users');
        const users = await response.json();
        renderUserList(users);
    } catch (error) {
        console.error('Error fetching users:', error);
    }
}

// Fetch user profile image
async function fetchUserProfile(profileId) {
    try {
        const response = await fetch(`/binary-contents/${profileId}`);
        if (!response.ok) throw new Error('Failed to fetch profile');
        const profile = await response.json();

        // Data is already base64 encoded string
        return `data:${profile.contentType};base64,${profile.data}`;
    } catch (error) {
        console.error('Error fetching profile:', error);
        return '/default-avatar.png'; // Fallback to default avatar
    }
}

// Render user list
async function renderUserList(users) {
    const userListElement = document.getElementById('userList');
    userListElement.innerHTML = ''; // Clear existing content

    if (users.length === 0) {
        userListElement.innerHTML = '<p style="color: white; text-align: center; padding: 40px;">등록된 사용자가 없습니다.</p>';
        return;
    }

    for (const user of users) {
        const userElement = document.createElement('div');
        userElement.className = 'user-item';

        // Get profile image URL
        const profileUrl = user.profileId ?
            await fetchUserProfile(user.profileId) :
            '/default-avatar.png';

        userElement.innerHTML = `
            <div class="user-content">
                <img src="${profileUrl}" alt="${user.username}" class="user-avatar">
                <div class="user-info">
                    <div class="user-name">${user.username}</div>
                    <div class="user-email">${user.email}</div>
                </div>
                <div class="status-badge ${user.online ? 'online' : 'offline'}">
                    ${user.online ? '온라인' : '오프라인'}
                </div>
            </div>
            <div class="user-actions">
                <button class="btn btn-edit" onclick="editUser('${user.id}')">✏️ 수정</button>
                <button class="btn btn-delete" onclick="deleteUser('${user.id}', '${user.username}')">🗑️ 삭제</button>
            </div>
        `;

        userListElement.appendChild(userElement);
    }
}

// Edit user
function editUser(userId) {
    window.location.href = `/user-edit.html?id=${userId}`;
}

// Delete user
async function deleteUser(userId, username) {
    if (!confirm(`정말로 "${username}" 사용자를 삭제하시겠습니까?`)) {
        return;
    }

    try {
        const response = await fetch(`/users/${userId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`Failed to delete user: ${response.status}`);
        }

        alert('✅ 사용자가 삭제되었습니다.');

        // Reload user list
        fetchAndRenderUsers();

    } catch (error) {
        console.error('Error deleting user:', error);
        alert('❌ 사용자 삭제 실패: ' + error.message);
    }
}
