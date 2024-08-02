// orderinfo.js
let userEmail = '';

document.addEventListener('DOMContentLoaded', function() {
    // 유저 아이디(이메일) 정보 가져오기
    const checkLoginStatus = () => {
        fetch('/board', {
            method: 'GET',
            credentials: 'include'
        })
            .then(response => response.json())
            .then(data => {
                if (data.loggedIn) {
                    // 유저 이메일(아이디) 저장
                    userEmail = data.userEmail;
                }
                else {  // 비 로그인 상태
                    window.alert("로그인이 필요합니다.");
                    window.location.href = 'login.html';
                }
            })
            .catch(error => console.error('Error:', error));
    };
    
    // 페이지 로드 시 로그인 상태 확인
    checkLoginStatus();

    // 주문내역가져오기
    fetchOrders();
});

function fetchOrders() {
    fetch('/orderinfo', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userEmail)
    })
        .then(response => response.json())
        .then(data => {
            const emptyList = document.querySelector('.empty-list');
            const orderTableBody = document.getElementById('orderTableBody');
            data.forEach(info => {
                // 데이터 없다는 텍스트 숨기기
                emptyList.style.display = "none";

                // 주문내역 출력
                const row = document.createElement('tr');
                row.classList.add('order-row');
                row.dataset.orderId = info.orderId; // 주문 ID를 데이터 속성으로 저장
                row.innerHTML = `
                    <td>${info.orderId}</td>
                    <td><img src="${info.image}" alt="${info.productName}" /></td>
                    <td>${info.productName} 외 ${info.count - 1}건</td>
                    <td>₩ ${info.totalAmount}</td>
                    <td>${info.created_at}</td>
                `;
                row.addEventListener('click', () => {
                    window.location.href = `/order-details.html?orderId=${info.orderId}`;
                });
                orderTableBody.appendChild(row);
            });
        })
        .catch(error => {
            emptyList.style.display = "block";
            console.error('Error fetching orders:', error)
        });
}
