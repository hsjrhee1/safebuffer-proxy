# SafeBuffer — 법률 증거 안전 버퍼 녹음 앱

## 개념

"항상 듣고 있되, 저장은 하지 않는다. 사건이 터지면 그때 저장한다."

- 백그라운드에서 AAC 16kbps M4A로 연속 녹음
- 48시간 후 자동 삭제 (잠금하지 않은 경우)
- 사건 발생 시 슬라이더로 구간 선택 → 잠금 → 영구 보존

## 프로젝트 구조

```
app/src/main/java/com/safebuffer/app/
├── data/
│   ├── local/
│   │   ├── ChunkEntity.kt       # Room 엔티티 (청크 메타데이터)
│   │   ├── ChunkDao.kt          # DB 쿼리
│   │   └── ChunkDatabase.kt     # Room DB 설정
│   └── repository/
│       └── AudioChunkRepository.kt  # 데이터 접근 레이어
├── di/
│   └── AppModule.kt             # Hilt DI 모듈
├── domain/
│   └── usecase/
│       └── LockChunksUseCase.kt # 구간 잠금 비즈니스 로직
├── receiver/
│   └── BootReceiver.kt          # 부팅 후 재시작
├── service/
│   └── RecordingService.kt      # ★ 핵심: Foreground Service 녹음
├── ui/
│   ├── MainActivity.kt          # 메인 화면
│   └── MainViewModel.kt         # UI 상태 관리
├── worker/
│   └── CleanupWorker.kt         # WorkManager 48시간 자동삭제
└── SafeBufferApp.kt             # Application (Hilt 진입점)
```

## 핵심 기술

| 컴포넌트 | 기술 | 이유 |
|---------|------|------|
| 백그라운드 녹음 | Foreground Service | Android 8+ 필수 |
| 오디오 코덱 | MediaRecorder + AAC 16kbps | 10분 = ~1.2 MB |
| 파일 회전 | 10분 청크 자동 교체 | 메모리 효율 |
| DB | Room | 청크 메타데이터 추적 |
| 자동삭제 | WorkManager (매 1시간) | 48시간 초과 청크 제거 |
| DI | Hilt | 의존성 관리 |

## 용량 계산

| 보관 기간 | 청크 수 | 용량 |
|----------|--------|------|
| 1시간 | 6개 | ~7.2 MB |
| 24시간 | 144개 | ~173 MB |
| 48시간 (최대) | 288개 | ~346 MB |

## 빌드 방법

1. Android Studio Hedgehog (2023.1.1) 이상에서 열기
2. `minSdk 26` (Android 8.0) 이상 기기 또는 에뮬레이터
3. Gradle Sync → Run

## Phase 2 로드맵

- [ ] Whisper API STT 연동 (잠긴 구간 자동 텍스트 변환)
- [ ] LexNote 연동 (STT 결과 → AI 파싱 → 일기 자동 첨부)
- [ ] iOS 대응 (외부 블루투스 마이크 + 앱 연동 방식)
- [ ] 부팅 후 자동 재시작 (BootReceiver 활성화)
