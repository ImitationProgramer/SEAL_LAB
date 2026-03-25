/**
 * SEAL LAB - Hero Section Live Globe Animation
 * 3D Rotating Network Globe with Canvas 2D
 */

const canvas = document.getElementById('heroCanvas');
const ctx = canvas.getContext('2d');

let width, height;
let points = [];        // 지구 위 지점들 (3D 좌표)
let connections = [];   // 지점 간 연결 라인
let pulses = [];        // 라인 위를 흐르는 데이터 점
let rotationAngle = 0;  // 지구의 현재 회전각

const GLOBE_RADIUS_RATIO = 0.7; // Hero 섹션 높이 대비 지구본 크기
const POINT_COUNT = 100;        // 지점 개수
const CONNECTION_COUNT = 120;   // 연결 선 개수

function init() {
    resize();
    createPoints();
    createConnections();
    animate();
}

function resize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = canvas.parentElement.offsetHeight;
}

// 1. 구체 위에 균일하게 점 생성 (Spherical Coordinates)
function createPoints() {
    points = [];
    const radius = Math.min(width, height) * GLOBE_RADIUS_RATIO * 0.5;

    for (let i = 0; i < POINT_COUNT; i++) {
        // 골든 섹션 방식을 이용한 구체 위 균일 분포
        const phi = Math.acos(-1 + (2 * i) / POINT_COUNT);
        const theta = Math.sqrt(POINT_COUNT * Math.PI) * phi;

        points.push({
            phi: phi,
            theta: theta,
            radius: radius
        });
    }
}

// 2. 랜덤한 지점 쌍 연결
function createConnections() {
    connections = [];
    for (let i = 0; i < CONNECTION_COUNT; i++) {
        const startIdx = Math.floor(Math.random() * points.length);
        const endIdx = Math.floor(Math.random() * points.length);

        if (startIdx !== endIdx) {
            connections.push({
                start: points[startIdx],
                end: points[endIdx],
                // 선마다 조금씩 다른 속도의 펄스 생성
                pulseSpeed: 0.002 + Math.random() * 0.004,
                pulseProgress: Math.random()
            });
        }
    }
}

// 3. 3D 좌표를 2D 화면 좌표로 변환 (Projection)
function project(point, angle) {
    const centerX = width / 2;
    const centerY = height / 2;

    // Y축 기준 회전 적용
    const rotatedTheta = point.theta + angle;

    // 3D 좌표 계산
    const x3d = point.radius * Math.sin(point.phi) * Math.cos(rotatedTheta);
    const y3d = point.radius * Math.cos(point.phi);
    const z3d = point.radius * Math.sin(point.phi) * Math.sin(rotatedTheta);

    // 원근감 (Perspective) 효과
    const perspective = 500 / (500 - z3d);

    return {
        x: x3d * perspective + centerX,
        y: y3d * perspective + centerY,
        z: z3d, // 깊이 값 (그리기 여부 판단용)
        scale: perspective
    };
}

function animate() {
    ctx.clearRect(0, 0, width, height);

    // 서서히 회전
    rotationAngle += 0.002;

    // 연결선 및 펄스 그리기
    connections.forEach(conn => {
        const p1 = project(conn.start, rotationAngle);
        const p2 = project(conn.end, rotationAngle);

        // 두 점 중 하나라도 카메라 앞쪽(z > -50)에 있을 때만 그리기 (뒤쪽 가림 효과)
        if (p1.z > -100 && p2.z > -100) {
            // 아치형 곡선을 위한 제어점 계산
            const cp = {
                x: (p1.x + p2.x) / 2,
                y: (p1.y + p2.y) / 2 - (Math.abs(p1.x - p2.x) * 0.2) // 위로 볼록하게
            };

            // 1. 연결 라인 그리기
            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.quadraticCurveTo(cp.x, cp.y, p2.x, p2.y);

            // 깊이에 따른 투명도 조절 (입체감)
            const opacity = Math.max(0.05, (p1.z + p2.z + 400) / 800 * 0.3);
            ctx.strokeStyle = `rgba(123, 166, 192, ${opacity})`;
            ctx.lineWidth = 0.8;
            ctx.stroke();

            // 2. 흐르는 펄스(데이터 점) 그리기
            conn.pulseProgress += conn.pulseSpeed;
            if (conn.pulseProgress > 1) conn.pulseProgress = 0;

            const t = conn.pulseProgress;
            // 2차 베지에 곡선 공식으로 현재 펄스 위치 계산
            const pulseX = Math.pow(1 - t, 2) * p1.x + 2 * (1 - t) * t * cp.x + Math.pow(t, 2) * p2.x;
            const pulseY = Math.pow(1 - t, 2) * p1.y + 2 * (1 - t) * t * cp.y + Math.pow(t, 2) * p2.y;

            // 앞쪽에 있을 때만 펄스를 더 밝게 표시
            if (p1.z > 0 || p2.z > 0) {
                ctx.beginPath();
                ctx.arc(pulseX, pulseY, 1.5 * p1.scale, 0, Math.PI * 2);
                ctx.fillStyle = `rgba(123, 166, 192, ${opacity * 3})`;
                ctx.shadowBlur = 5;
                ctx.shadowColor = "#7ba6c0";
                ctx.fill();
                ctx.shadowBlur = 0; // 그림자 초기화 (성능 저하 방지)
            }
        }
    });

    // 지점(Node) 그리기
    points.forEach(point => {
        const p = project(point, rotationAngle);
        if (p.z > 0) { // 앞쪽에 있는 점만 그림
            ctx.beginPath();
            ctx.arc(p.x, p.y, 1.2 * p.scale, 0, Math.PI * 2);
            ctx.fillStyle = "rgba(123, 166, 192, 0.4)";
            ctx.fill();
        }
    });

    requestAnimationFrame(animate);
}

// 브라우저 이벤트 리스너
window.addEventListener('resize', () => {
    resize();
    createPoints();
    createConnections();
});

// 초기 실행
window.addEventListener('load', init);