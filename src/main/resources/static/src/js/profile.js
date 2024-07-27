document.addEventListener('DOMContentLoaded', () => {
    // 메뉴 요소 클릭 시 data-url 값으로 이동
    const profileLinks = document.querySelectorAll('.profile-link');
    profileLinks.forEach(link => {
        link.addEventListener('click', () => {
            const url = link.getAttribute('data-url');
            if (url) {
                window.location.href = url;
            }
        });
    });

    // 유저 정보 가져오기
    const fetchUserInfo = async () => {
        try {
            const response = await fetch('/profile', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (response.ok) {
                const userInfo = await response.json();
                updateProfileInfo(userInfo);
            } else {
                console.error('Failed to fetch user info');
            }
        } catch (error) {
            console.error('Error fetching user info:', error);
        }
    };

    // 프로필 정보 업데이트
    const updateProfileInfo = (userInfo) => {
        const profileInfoContainer = document.querySelector('.profile-info');
        const nameElement = profileInfoContainer.querySelector('h2');
        const nationalityElement = profileInfoContainer.querySelector('p');
        const userEmail = document.getElementById('user-email');

        nameElement.textContent = `${userInfo.nickname}`;
        nationalityElement.textContent = `국적 : ${userInfo.nationality}`;
        userEmail.textContent = `아이디 : ${userInfo.email}`;
    };

    // 로그아웃 버튼 클릭 이벤트 처리
    const logoutButton = document.querySelector('.logout-btn');
    if (logoutButton) {
        logoutButton.addEventListener('click', async () => {
            try {
                const response = await fetch('/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'include'
                });

                if (response.ok) {
                    // 로그아웃 성공 시 쿠키 삭제 및 로그인 페이지로 리디렉션
                    document.cookie = 'authToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
                    alert('로그아웃 되었습니다.');
                    window.location.href = 'networker.html';
                } else {
                    const errorData = await response.json();
                    alert(errorData.message || '로그아웃에 실패했습니다.');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('로그아웃 중 오류가 발생했습니다.');
            }
        });
    }

    // 페이지 로드 시 유저 정보 가져오기
    fetchUserInfo();
});
