# Matchora — kaynak envanteri

Tarih: 2026-11-10. `python3 scripts/ui_backend_inventory.py` ile üretildi.

**56 ortak UI dosyası**, **263 fonksiyon bildirimi**, **95 pencere/menü açılış noktası**, **21 native Kotlin dosyası**, **111 sunucu komutu**.

Fonksiyon sayısı ekran sayısı değildir; yardımcı çizim, yerel callback ve `expect` bildirimleri de dahildir. Pencere sayısı farklı kullanıcı akışlarının sayısı değildir; ortak bir pencere birçok yerde kullanılır. Bu tarama koddan envanter çıkarır, cihaz testi veya bütün davranışların tamamlandığı iddiası değildir.

Kullanıcıya görünen alanlar ve yapılmış/yapılmamış bağlantılar: [tam ekran envanteri](TUM_EKRANLAR_BACKEND_ENVANTERI.md).

## Ortak UI dosyaları ve tüm fonksiyonlar

| Dosya | Fonksiyonlar | Pencere/menü noktaları |
|---|---|---|
| [App.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/App.kt) | `nextUniqueId`:47, `App`:57, `MainAppShell`:80, `requireAccount`:202, `showToast`:213, `remote`:217, `remoteProfile`:237, `remotePhoto`:260, `remoteMemoryEdit`:279, `remotePhotoVerification`:299, `remoteLogout`:319, `finishAuthentication`:331, `completeAuthentication`:341, `openEventById`:373, `openCatalogTarget`:389, `showTarget`:391, `activateDemoPlan`:533, `lockTokens`:543, `releaseTokens`:555, `releaseEventTokens`:562, `joinEventImmediately`:568, `openDirectChatWithPlayer`:601, `approveJoinRequest`:638, `rejectJoinRequest`:665, `submitJoinRequest`:674, `cancelJoinRequest`:716, `createTeam`:727, `sendTeamChallenge`:733, `acceptTeamChallenge`:747, `declineTeamChallenge`:782, `applyToTeam`:790, `approveTeamJoinRequest`:805, `rejectTeamJoinRequest`:818, `FloatingAppDock`:1809, `FloatingPlayButton`:1837, `DockNavItem`:1877 | `Popup`:1252, `Popup`:1256, `AlertDialog`:1264, `AlertDialog`:1267 |
| [AccountAndActivitySheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/AccountAndActivitySheets.kt) | `ActivitySheet`:30, `AccountSettingsSheet`:42, `SettingsGroup`:171, `PreferenceRow`:176, `MyActivitySheet`:185, `MatchResultSheet`:224, `ContactVerificationSheet`:273, `PhotoVerificationSheet`:323, `PlayerFeedbackSheet`:338 | `ModalBottomSheet`:31, `AlertDialog`:155, `AlertDialog`:159, `AlertDialog`:160, `ModalBottomSheet`:191, `AlertDialog`:219 |
| [AppLinks.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/AppLinks.kt) | `AppLinkButton`:39, `AppLegalLinks`:71, `LegalReviewButton`:82 | `AlertDialog`:67, `AlertDialog`:91 |
| [ApplyToTeamSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/ApplyToTeamSheet.kt) | `ApplyToTeamSheet`:38 | `ModalBottomSheet`:58 |
| [AthleteQrPassSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/AthleteQrPassSheet.kt) | `AthleteQrPassSheet`:37, `refresh`:45 | `ModalBottomSheet`:52 |
| [AuthAndOnboardingSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/AuthAndOnboardingSheet.kt) | `AuthAndOnboardingSheet`:50, `continueToPassport`:80, `enter`:98, `sendEmailCode`:114, `LoginOptionsView`:195, `ProviderButton`:239, `AuthHeader`:251, `PassportSetupView`:266, `EmailInputView`:386, `EmailOtpView`:399, `ProfileEditField`:414, `TimePickerMenu`:451, `AppleLogoIcon`:566, `GoogleLogoIcon`:573 | `ModalBottomSheet`:131, `DropdownMenu`:519 |
| [BackendEditingActions.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/BackendEditingActions.kt) | `BackendEventEditing`:18, `BackendTeamEditing`:64, `BackendChallengeActions`:105 | `AlertDialog`:45, `AlertDialog`:87, `AlertDialog`:110 |
| [BackendManagementActions.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/BackendManagementActions.kt) | `BackendPlayerSafetyActions`:21, `BackendChatMuteAction`:45, `BackendIncomingTeamInvites`:55, `rememberBackendAction`:66, `BackendTeamActions`:92, `BackendCommunityActions`:148, `BackendTournamentActions`:222, `BackendTournamentRegistrationActions`:255 | `AlertDialog`:34, `AlertDialog`:38, `AlertDialog`:83, `AlertDialog`:116, `AlertDialog`:131, `AlertDialog`:134, `AlertDialog`:137, `AlertDialog`:140, `AlertDialog`:168, `AlertDialog`:180, `AlertDialog`:188, `AlertDialog`:195, `AlertDialog`:203, `AlertDialog`:244, `AlertDialog`:263 |
| [BackendMoreCatalogButton.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/BackendMoreCatalogButton.kt) | `BackendMoreCatalogButton`:13 | Yok |
| [BackendMoreEventsButton.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/BackendMoreEventsButton.kt) | `BackendMoreEventsButton`:12 | Yok |
| [BackendPhoto.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/BackendPhoto.kt) | `BackendPhoto`:22, `playerAvatarUploadId`:41 | Yok |
| [CheckInCodeActions.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CheckInCodeActions.kt) | `makeQrCode`:14, `readQrCode`:15, `CheckInCodeActions`:18 | `AlertDialog`:37 |
| [CityAndLocationSelectorSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CityAndLocationSelectorSheet.kt) | `CityAndLocationSelectorSheet`:38 | `ModalBottomSheet`:64, `ExposedDropdownMenu`:84, `AlertDialog`:99 |
| [CommonUi.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CommonUi.kt) | `MCard`:52, `MScreenHeaderCard`:95, `MModuleCard`:155, `MSubCard`:246, `MSectionHeader`:282, `MBadge`:351, `MChemistryBadge`:391, `MChip`:432, `MButton`:474, `MTopBar`:545, `ExploreMenuSheet`:666, `MToastBanner`:854, `MSegmentedControl`:959 | `ModalBottomSheet`:671 |
| [CompetitionHighlights.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CompetitionHighlights.kt) | `AthleteAwardCard`:28, `MatchScouterCard`:49, `CreateCupCard`:70, `CreateClubCard`:87 | Yok |
| [CreateCommunitySheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CreateCommunitySheet.kt) | `CreateCommunitySheet`:15 | Yok |
| [CreateEventAndChallengeSheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CreateEventAndChallengeSheets.kt) | `CreateEventSheet`:36, `TeamChallengeSheet`:287, `ReportAndDisputeDialog`:519 | `ModalBottomSheet`:54, `ModalBottomSheet`:310, `AlertDialog`:527 |
| [CreateTeamSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CreateTeamSheet.kt) | `CreateTeamSheet`:38 | `ModalBottomSheet`:72 |
| [CreateTournamentSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/CreateTournamentSheet.kt) | `CreateTournamentSheet`:38, `formatLabel`:57 | `ModalBottomSheet`:81 |
| [DeviceServices.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/DeviceServices.kt) | `freshLocationFix`:11, `canonicalRegionCity`:16, `key`:18, `approximateDistanceKm`:24, `rememberLocationRequester`:37, `rememberRegionCenter`:41, `rememberNotificationPermissionRequester`:44, `openApplicationSettings`:46, `shareAccountExport`:48, `rememberAppActive`:51, `NotificationPermissionButton`:54, `hasForegroundLocationPermission`:67, `rememberForegroundLocationRefresh`:71 | `AlertDialog`:61 |
| [DockCutoutShape.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/DockCutoutShape.kt) | `createOutline`:10 | Yok |
| [EventDateTimePicker.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/EventDateTimePicker.kt) | `EventDateTimeField`:15, `isSelectableDate`:26, `isSelectableYear`:27 | `DatePickerDialog`:29, `AlertDialog`:38 |
| [EventDetailSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/EventDetailSheet.kt) | `EventDetailSheet`:36 | `AlertDialog`:80, `AlertDialog`:84, `AlertDialog`:101, `ModalBottomSheet`:108 |
| [EventDeviceActions.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/EventDeviceActions.kt) | `eventInvitationId`:9, `eventInvitationText`:20, `isOnlineMeeting`:26, `eventDirectionsPoint`:31, `eventCalendarNotes`:34, `shareEventInvitation`:43, `openEventDirections`:44, `openEventCalendar`:45, `EventDeviceActions`:48 | `AlertDialog`:52 |
| [EventLocationPicker.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/EventLocationPicker.kt) | `LocationSelectionMap`:17, `EventLocationPicker`:21 | `ModalBottomSheet`:53 |
| [InteractiveActionSheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/InteractiveActionSheets.kt) | `AutoSquadSheet`:44, `SquadSummaryTile`:197, `PlayerProfileSheet`:216, `VenueBookingSheet`:631, `TournamentDetailSheet`:716, `SkillBreakdownDialog`:1212, `BadgeDetailDialog`:1282 | `ModalBottomSheet`:56, `ModalBottomSheet`:259, `ModalBottomSheet`:641, `ModalBottomSheet`:752, `AlertDialog`:1163, `AlertDialog`:1185, `AlertDialog`:1223, `AlertDialog`:1290 |
| [ManageJoinRequestsSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/ManageJoinRequestsSheet.kt) | `ManageJoinRequestsSheet`:35 | `ModalBottomSheet`:50 |
| [ManageTeamRequestsSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/ManageTeamRequestsSheet.kt) | `ManageTeamRequestsSheet`:36 | `ModalBottomSheet`:44 |
| [MatchoraWalletCard.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/MatchoraWalletCard.kt) | `MatchoraWalletCard`:23 | Yok |
| [NativeMapView.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/NativeMapView.kt) | `validMapCoordinate`:8, `mapZoomForMeters`:12, `NativeMapView`:29 | Yok |
| [NetworkSpotlightCards.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/NetworkSpotlightCards.kt) | `SpotlightTitle`:31, `SpotlightStats`:60, `CommunitySpotlightCard`:89, `RankingSpotlightCard`:127, `TournamentSpotlightCard`:170, `TeamSpotlightCard`:184, `ChallengeSpotlightCard`:199 | Yok |
| [NotificationsSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/NotificationsSheet.kt) | `NotificationDeliveryControls`:45, `matchesFilter`:60, `NotificationsSheet`:80, `NotificationPreference`:248, `NotificationInboxCard`:263 | `AlertDialog`:90, `ModalBottomSheet`:118 |
| [OperationsSheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/OperationsSheets.kt) | `OperatorActionDialog`:24, `ModerationSheet`:36, `VenueOperatorSheet`:146, `CreateVenueSheet`:221, `VenueScheduleSheet`:259, `CreateVenueSlotSheet`:285, `venuePriceMinor`:308 | `AlertDialog`:27 |
| [PhotoPicker.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/PhotoPicker.kt) | `rememberNativeGalleryPicker`:8, `rememberNativeCameraPicker`:11, `rememberGalleryPhotoPicker`:15 | `AlertDialog`:20, `AlertDialog`:26 |
| [PlayNowSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/PlayNowSheet.kt) | `formatTime`:46, `parseClockMinutes`:52, `PlayNowModalSheet`:68 | `ModalBottomSheet`:166 |
| [PremiumUi.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/PremiumUi.kt) | `premiumSurface`:46, `pressMotion`:55, `MFeatureHeader`:63, `MPremiumSheetHandle`:91, `MSheetHeading`:99, `MFloatingInputBox`:176, `MFloatingDropdownBox`:350, `MFloatingActionRow`:523, `premiumTextFieldColors`:627, `premiumChoiceChipColors`:642 | `DropdownMenu`:464 |
| [RatingAndMissionSheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/RatingAndMissionSheets.kt) | `RatingPolicyButton`:15, `RatingPolicySheet`:22, `RatingStatusCard`:44, `MissionsSheet`:76 | Yok |
| [SendTeamChallengeSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/SendTeamChallengeSheet.kt) | `SendTeamChallengeSheet`:49, `MatchChallengeDetailSheet`:240, `AiMatchScouterSheet`:406, `PremiumMatchupPanel`:572 | `ModalBottomSheet`:89, `ModalBottomSheet`:256, `ModalBottomSheet`:427 |
| [SportPicker.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/SportPicker.kt) | `searchKey`:46, `SportPickerField`:50, `SportPickerSheet`:106, `SportOptionRow`:175 | `ModalBottomSheet`:121 |
| [SportsFlowBackground.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/SportsFlowBackground.kt) | `SportsFlowBackground`:70, `position`:162, `visibility`:171, `scaleDraw`:234, `drawTennisCourt`:240, `drawGoal`:260, `drawHoop`:284, `drawGoalkeeperGlove`:309, `drawRacket`:329, `drawDumbbell`:341, `drawGoggles`:349, `drawShuttle`:357, `drawSkate`:367 | Yok |
| [SubmitJoinRequestSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/SubmitJoinRequestSheet.kt) | `SubmitJoinRequestSheet`:35 | `ModalBottomSheet`:59 |
| [TeamDetailSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/TeamDetailSheet.kt) | `TeamDetailSheet`:41 | `ModalBottomSheet`:75 |
| [TokenTopUpCardSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/TokenTopUpCardSheet.kt) | `TokenTopUpCardSheet`:7 | Yok |
| [TournamentRegistrationSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/TournamentRegistrationSheet.kt) | `TournamentRegistrationSheet`:14 | Yok |
| [VenueChoiceField.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/VenueChoiceField.kt) | `venueChoiceValue`:10, `VenueChoiceField`:22 | Yok |
| [VenueReviewSheets.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/VenueReviewSheets.kt) | `VenueReviewsSheet`:18, `VenueReviewEditorSheet`:70 | `AlertDialog`:85 |
| [VenueSpotlightCard.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/VenueSpotlightCard.kt) | `VenueSpotlightCard`:32 | Yok |
| [WalletAndPaymentSheet.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/components/WalletAndPaymentSheet.kt) | `WalletAndPaymentSheet`:44, `TokenWalletSheet`:70, `price`:102, `storeAction`:103 | `ModalBottomSheet`:119, `AlertDialog`:553 |
| [ChatHubScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/ChatHubScreen.kt) | `ChatHubScreen`:67, `open`:137, `OlderMessagesAction`:912, `MiniMemoryStoryTile`:937, `HeroMemoryExploreCard`:1001, `GridMemoryExploreTile`:1154, `ChatAttachedPhotoBubble`:1270, `MatchMemoryVisualCanvas`:1329, `CreateMemoryPostSheet`:1398, `MemoryDetailSheet`:1629, `ChatAvatar`:1816 | `DropdownMenu`:475, `AlertDialog`:828, `AlertDialog`:890, `AlertDialog`:898, `AlertDialog`:900, `AlertDialog`:902, `ModalBottomSheet`:1443, `AlertDialog`:1640, `ModalBottomSheet`:1645 |
| [CommunityAndVenueScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/CommunityAndVenueScreen.kt) | `CommunityAndVenueScreen`:39 | Yok |
| [CompetitionScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/CompetitionScreen.kt) | `CompetitionScreen`:52, `requestChallenge`:168 | `AlertDialog`:468, `AlertDialog`:626, `AlertDialog`:631 |
| [DiscoverMapScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/DiscoverMapScreen.kt) | `DiscoverMapScreen`:56, `demoCoordinatesForArea`:723 | Yok |
| [HomeScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/HomeScreen.kt) | `HomeScreen`:64, `SmartMatchSheet`:914, `LiveMiniRadarView`:1136, `ExpandableMatchCard`:1295, `demoCoordinatesForArea`:1615, `CompactEventDirectory`:1628, `NetworkScopeIsland`:1713, `feedCardFinish`:1742 | `ModalBottomSheet`:931 |
| [SplashScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/SplashScreen.kt) | `createOutline`:56, `MatchoraSplashScreen`:90, `startKnifeSlice`:161, `SplashContent`:359 | Yok |
| [SportsPassportScreen.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/screens/SportsPassportScreen.kt) | `SportsPassportScreen`:49, `SkillProfileEditorSheet`:736, `ProfileEditorSheet`:833, `ProfileEditorSectionTitle`:1033, `ProfileEditField`:1045, `PassportMetric`:1081, `VerificationChip`:1106, `ExpandPassportRows`:1139, `PassportSectionCard`:1169, `GuestPassportView`:1214, `GuestPerkRow`:1537 | `ModalBottomSheet`:744, `ModalBottomSheet`:871, `AlertDialog`:1019 |
| [Theme.kt](../composeApp/src/commonMain/kotlin/com/matchora/app/ui/theme/Theme.kt) | `MatchoraTheme`:115 | Yok |

## Native Kotlin uygulamaları

| Dosya | Fonksiyonlar |
|---|---|
| [composeApp/src/androidMain/kotlin/com/matchora/app/MainActivity.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/MainActivity.kt) | `onCreate`, `onNewIntent`, `handleNotification` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/data/BackendTransport.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/data/BackendTransport.android.kt) | `executeApiRequest`, `key`, `preferences`, `read`, `write`, `encodeUploadPhoto`, `decodeApiPhoto` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/data/BrowserAuthentication.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/data/BrowserAuthentication.android.kt) | `authenticateInBrowser` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/data/MatchoraMessagingService.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/data/MatchoraMessagingService.kt) | `initializePush`, `onNewToken`, `onMessageReceived` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/data/StorePlatform.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/data/StorePlatform.android.kt) | Sınıf / callback uygulaması |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/CheckInCodeActions.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/CheckInCodeActions.android.kt) | `makeQrCode`, `readQrCode` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/DeviceServices.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/DeviceServices.android.kt) | `shareAccountExport`, `rememberAppActive`, `openApplicationSettings`, `boundedGeocode`, `rememberRegionCenter`, `rememberLocationRequester`, `valid`, `onLocationChanged`, `onProviderDisabled`, `onProviderEnabled`, `onStatusChanged`, `rememberNotificationPermissionRequester`, `hasForegroundLocationPermission` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/EventDeviceActions.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/EventDeviceActions.android.kt) | `shareEventInvitation`, `openEventDirections`, `openEventCalendar` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/LocationSelectionMap.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/LocationSelectionMap.android.kt) | `LocationSelectionMap` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/NativeMapView.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/NativeMapView.android.kt) | `NativeMapView`, `AndroidLiveMap`, `start`, `resume`, `pause`, `stop`, `onConfigurationChanged`, `onLowMemory`, `onTrimMemory`, `PreviewNativeMapView`, `drawCleanRoad` |
| [composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/PhotoPicker.android.kt](../composeApp/src/androidMain/kotlin/com/matchora/app/ui/components/PhotoPicker.android.kt) | `readPhoto`, `rememberNativeGalleryPicker`, `rememberNativeCameraPicker` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/MainViewController.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/MainViewController.kt) | `MainViewController` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/data/BackendTransport.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/data/BackendTransport.ios.kt) | `executeApiRequest`, `query`, `read`, `write`, `encodeUploadPhoto`, `decodeApiPhoto` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/data/BrowserAuthentication.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/data/BrowserAuthentication.ios.kt) | `authenticateInBrowser`, `presentationAnchorForWebAuthenticationSession` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/data/StorePlatform.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/data/StorePlatform.ios.kt) | Sınıf / callback uygulaması |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/CheckInCodeActions.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/CheckInCodeActions.ios.kt) | `qrData`, `makeQrCode`, `readQrCode` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/DeviceServices.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/DeviceServices.ios.kt) | `shareAccountExport`, `rememberAppActive`, `openApplicationSettings`, `rememberRegionCenter`, `start`, `cancel`, `finish`, `authorize`, `locationManagerDidChangeAuthorization`, `locationManager`, `locationManager`, `rememberLocationRequester`, `rememberNotificationPermissionRequester`, `hasForegroundLocationPermission` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/EventDeviceActions.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/EventDeviceActions.ios.kt) | `eventPresenter`, `shareEventInvitation`, `openEventDirections`, `openEventCalendar`, `eventEditViewController` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/LocationSelectionMap.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/LocationSelectionMap.ios.kt) | `mapView`, `LocationSelectionMap` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/NativeMapView.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/NativeMapView.ios.kt) | `mapView`, `mapView`, `NativeMapView` |
| [composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/PhotoPicker.ios.kt](../composeApp/src/iosMain/kotlin/com/matchora/app/ui/components/PhotoPicker.ios.kt) | `photoBytes`, `preparePhoto`, `topController`, `close`, `rememberNativeGalleryPicker`, `picker`, `rememberNativeCameraPicker`, `imagePickerController`, `imagePickerControllerDidCancel` |

## Komut dışındaki HTTP uç noktaları

Kaynak: `backend/src/server.mjs`. Bu yollar 111 komut sayısına dahil değildir.

| Yöntem | Yol / grup | Amaç |
|---|---|---|
| GET | `/health`, `/openapi.json` | Sağlık ve komut sözleşmesi |
| GET | `/v1/config`, `/v1/bootstrap` | Yapılandırma ve kapsamlı başlangıç verisi |
| GET | `/v1/events`, `/v1/events/:id` | Maç listesi ve erişim kontrollü ayrıntı |
| GET | `/v1/players/:id?sport=...` | Gizlilik kontrollü sporcu formu/uyum |
| GET | `/v1/catalog/:kind`, `/v1/notifications` | Katalog ve kişisel bildirim sayfaları |
| GET | `/v1/memories`, `/v1/rankings` | Küresel vitrin ve filtrelenmiş sıralama sayfaları |
| GET | `/v1/threads/:id/messages` | Üyeliğe bağlı mesaj sayfalaması |
| GET / POST | `/v1/media/:id` / `/v1/media` | Yetkili görsel okuma / yükleme |
| GET | `/v1/passports/verify?code=...` | İmzalı pasaport doğrulama |
| GET | `/v1/account/export`, `/v1/account/sessions` | Kendi verisi ve oturumlar |
| GET | `/v1/admin/inbox` | Güncel yönetici yetkisiyle moderasyon kuyruğu |
| GET | `/v1/operator/venues`, `/v1/venues/:id/reviews` | Tesis yönetimi / erişim kontrollü değerlendirmeler |
| POST | `/v1/auth/oauth/start`, `/v1/auth/oauth/finish`, `/v1/auth/exchange` | Sağlayıcı girişi |
| POST | `/v1/auth/otp/start`, `/v1/auth/otp/verify` | Telefonla giriş |
| POST | `/v1/auth/contact/start`, `/v1/auth/contact/verify` | Mevcut hesap telefonu |
| POST | `/v1/auth/refresh`, `/v1/auth/logout`, `/v1/auth/dev` | Oturum; dev yalnız yerel ortam |
| POST | `/v1/billing/revenuecat`, `/v1/billing/webhook` | Doğrulanmış mağaza bildirimi |
| POST | `/v1/commands/:operation` | Aşağıdaki yetkili işlemler |
| OPTIONS | İzinli CORS kaynakları | Preflight; işlem yetkisi vermez |

Swift uygulama delegesi, plist/manifest ve entitlements ayrıca ana envanterin cihaz/yayın bölümündedir.

## Bütün sunucu komutları

İstemci referansı yalnız sabit metin taramasıdır; dinamik çağrıya veya başarıya kanıt değildir. Referanssız komut otomatik olarak eksik sayılmaz; operatör veya sunucu işi olabilir.

| Komut | Sunucu kaynağı | Mobil sabit referansları |
|---|---|---|
| `account.delete` | [identity.mjs:83](../backend/src/identity.mjs) | `App.kt`:1776 |
| `account.delete.cancel` | [identity.mjs:89](../backend/src/identity.mjs) | `AccountAndActivitySheets.kt`:149 |
| `account.session.revoke` | [account.mjs:30](../backend/src/account.mjs) | `AccountAndActivitySheets.kt`:164 |
| `admin.announcement.send` | [operations.mjs:115](../backend/src/operations.mjs) | `OperationsSheets.kt`:67 |
| `admin.feature.set` | [admin.mjs:11](../backend/src/admin.mjs) | `OperationsSheets.kt`:72 |
| `admin.legal.publish` | [legal.mjs:11](../backend/src/legal.mjs) | Sabit mobil referans yok — API/operatör/sunucu akışı kontrol edilmeli |
| `admin.memory.review` | [admin.mjs:7](../backend/src/admin.mjs) | `OperationsSheets.kt`:106 |
| `admin.operator.assign` | [operations.mjs:168](../backend/src/operations.mjs) | `OperationsSheets.kt`:134 |
| `admin.outbox.retry` | [operations.mjs:175](../backend/src/operations.mjs) | `OperationsSheets.kt`:130 |
| `admin.report.resolve` | [admin.mjs:5](../backend/src/admin.mjs) | `OperationsSheets.kt`:96 |
| `admin.result.resolve` | [admin.mjs:9](../backend/src/admin.mjs) | `OperationsSheets.kt`:122 |
| `admin.support.resolve` | [admin.mjs:6](../backend/src/admin.mjs) | `OperationsSheets.kt`:92, `OperationsSheets.kt`:111 |
| `admin.user.status` | [admin.mjs:10](../backend/src/admin.mjs) | `OperationsSheets.kt`:135 |
| `admin.venue.application.review` | [venue-applications.mjs:25](../backend/src/venue-applications.mjs) | `OperationsSheets.kt`:87 |
| `admin.venue.review` | [operations.mjs:149](../backend/src/operations.mjs) | `OperationsSheets.kt`:126 |
| `admin.verification.photo` | [admin.mjs:8](../backend/src/admin.mjs) | `OperationsSheets.kt`:114 |
| `billing.sync` | [store-sync.mjs:39](../backend/src/store-sync.mjs) | `StoreBilling.kt`:64 |
| `booking.cancel` | [network.mjs:16](../backend/src/network.mjs) | `App.kt`:1782, `OperationsSheets.kt`:205 |
| `booking.complete` | [management.mjs:39](../backend/src/management.mjs) | `OperationsSheets.kt`:204 |
| `booking.confirm` | [network.mjs:17](../backend/src/network.mjs) | `OperationsSheets.kt`:202 |
| `booking.create` | [network.mjs:15](../backend/src/network.mjs) | `App.kt`:227, `App.kt`:1055 |
| `booking.payment.record` | [network.mjs:18](../backend/src/network.mjs) | `OperationsSheets.kt`:203 |
| `booking.refund.record` | [operations.mjs:156](../backend/src/operations.mjs) | `OperationsSheets.kt`:206 |
| `challenge.cancel` | [management.mjs:22](../backend/src/management.mjs) | `BackendEditingActions.kt`:110 |
| `challenge.create` | [competition.mjs:63](../backend/src/competition.mjs) | `App.kt`:734 |
| `challenge.respond` | [competition.mjs:64](../backend/src/competition.mjs) | `App.kt`:748, `App.kt`:783 |
| `chat.clear` | [chat-management.mjs:47](../backend/src/chat-management.mjs) | `App.kt`:1116 |
| `chat.delete` | [chat-management.mjs:48](../backend/src/chat-management.mjs) | `App.kt`:1118 |
| `chat.direct` | [social.mjs:12](../backend/src/social.mjs) | `App.kt`:602 |
| `chat.mute` | [social.mjs:15](../backend/src/social.mjs) | `BackendManagementActions.kt`:49 |
| `chat.read` | [social.mjs:14](../backend/src/social.mjs) | `App.kt`:829, `App.kt`:1128, `App.kt`:1401 |
| `chat.send` | [social.mjs:13](../backend/src/social.mjs) | `App.kt`:271, `App.kt`:1105, `App.kt`:1135 |
| `community.archive` | [community-management.mjs:20](../backend/src/community-management.mjs) | `BackendManagementActions.kt`:169 |
| `community.create` | [network.mjs:8](../backend/src/network.mjs) | `App.kt`:1050 |
| `community.invite` | [network.mjs:12](../backend/src/network.mjs) | `BackendManagementActions.kt`:206 |
| `community.invite.decline` | [management.mjs:37](../backend/src/management.mjs) | `BackendManagementActions.kt`:165 |
| `community.join` | [network.mjs:9](../backend/src/network.mjs) | `App.kt`:1065, `BackendManagementActions.kt`:164 |
| `community.leave` | [network.mjs:10](../backend/src/network.mjs) | `App.kt`:1065 |
| `community.member.remove` | [community-management.mjs:45](../backend/src/community-management.mjs) | `BackendManagementActions.kt`:195 |
| `community.review` | [network.mjs:11](../backend/src/network.mjs) | `BackendManagementActions.kt`:215, `BackendManagementActions.kt`:216 |
| `community.transfer` | [management.mjs:36](../backend/src/management.mjs) | `BackendManagementActions.kt`:206 |
| `community.update` | [community-management.mjs:34](../backend/src/community-management.mjs) | `BackendManagementActions.kt`:184 |
| `device.register` | [identity.mjs:91](../backend/src/identity.mjs) | `App.kt`:454 |
| `device.remove` | [identity.mjs:97](../backend/src/identity.mjs) | Sabit mobil referans yok — API/operatör/sunucu akışı kontrol edilmeli |
| `discovery.location` | [identity.mjs:66](../backend/src/identity.mjs) | `App.kt`:444 |
| `discovery.save` | [identity.mjs:73](../backend/src/identity.mjs) | `App.kt`:1702, `CommunityAndVenueScreen.kt`:70, `CommunityAndVenueScreen.kt`:126 |
| `event.checkin` | [events.mjs:39](../backend/src/events.mjs) | `App.kt`:1274 |
| `event.checkin.code` | [events.mjs:38](../backend/src/events.mjs) | `App.kt`:1272 |
| `event.create` | [events.mjs:8](../backend/src/events.mjs) | `App.kt`:227, `App.kt`:1523, `App.kt`:1580 |
| `event.join` | [events.mjs:31](../backend/src/events.mjs) | `App.kt`:227, `App.kt`:570, `App.kt`:1329 |
| `event.leave` | [events.mjs:32](../backend/src/events.mjs) | `App.kt`:1329 |
| `event.member.remove` | [event-management.mjs:14](../backend/src/event-management.mjs) | `App.kt`:1316 |
| `event.member.restore` | [event-management.mjs:30](../backend/src/event-management.mjs) | `App.kt`:1323 |
| `event.request` | [events.mjs:33](../backend/src/events.mjs) | `App.kt`:675 |
| `event.request.cancel` | [events.mjs:34](../backend/src/events.mjs) | `App.kt`:717 |
| `event.request.review` | [events.mjs:35](../backend/src/events.mjs) | `App.kt`:639, `App.kt`:666 |
| `event.transition` | [events.mjs:37](../backend/src/events.mjs) | `App.kt`:1422, `App.kt`:1426, `App.kt`:1431 |
| `event.update` | [management.mjs:6](../backend/src/management.mjs) | `BackendEditingActions.kt`:57 |
| `event.waitlist` | [events.mjs:36](../backend/src/events.mjs) | `App.kt`:949, `App.kt`:1354, `App.kt`:1355 |
| `feedback.submit` | [events.mjs:45](../backend/src/events.mjs) | `App.kt`:1797 |
| `legal.accept` | [legal.mjs:10](../backend/src/legal.mjs) | `BackendRepository.kt`:396 |
| `memory.create` | [social.mjs:17](../backend/src/social.mjs) | `App.kt`:270 |
| `memory.delete` | [social.mjs:19](../backend/src/social.mjs) | `App.kt`:1100, `OperationsSheets.kt`:99 |
| `memory.like` | [social.mjs:18](../backend/src/social.mjs) | `App.kt`:1199 |
| `memory.update` | [memory-management.mjs:6](../backend/src/memory-management.mjs) | `App.kt`:288 |
| `message.delete` | [chat-management.mjs:60](../backend/src/chat-management.mjs) | `App.kt`:1114 |
| `message.edit` | [chat-management.mjs:53](../backend/src/chat-management.mjs) | `App.kt`:1111 |
| `message.hide` | [chat-management.mjs:49](../backend/src/chat-management.mjs) | `App.kt`:1114 |
| `notification.clear` | [social.mjs:9](../backend/src/social.mjs) | `App.kt`:1626 |
| `notification.read` | [identity.mjs:90](../backend/src/identity.mjs) | `App.kt`:1604, `App.kt`:1627 |
| `passport.code` | [passport.mjs:6](../backend/src/passport.mjs) | `AthleteQrPassSheet.kt`:46 |
| `preferences.save` | [identity.mjs:48](../backend/src/identity.mjs) | `App.kt`:1601, `App.kt`:1767, `App.kt`:1768 |
| `profile.save` | [identity.mjs:24](../backend/src/identity.mjs) | `App.kt`:251, `App.kt`:310, `App.kt`:352 |
| `report.create` | [social.mjs:20](../backend/src/social.mjs) | `App.kt`:1049, `App.kt`:1654, `BackendManagementActions.kt`:40 |
| `result.confirm` | [events.mjs:43](../backend/src/events.mjs) | `App.kt`:1794 |
| `result.dispute` | [events.mjs:44](../backend/src/events.mjs) | `App.kt`:1791 |
| `result.submit` | [events.mjs:40](../backend/src/events.mjs) | `App.kt`:1789 |
| `support.create` | [identity.mjs:74](../backend/src/identity.mjs) | `App.kt`:1774 |
| `team.archive` | [competition.mjs:61](../backend/src/competition.mjs) | `BackendManagementActions.kt`:135 |
| `team.create` | [competition.mjs:54](../backend/src/competition.mjs) | `App.kt`:728 |
| `team.invite` | [competition.mjs:57](../backend/src/competition.mjs) | `App.kt`:993, `BackendManagementActions.kt`:127 |
| `team.invite.respond` | [competition.mjs:58](../backend/src/competition.mjs) | `BackendManagementActions.kt`:105, `BackendManagementActions.kt`:106 |
| `team.leave` | [competition.mjs:59](../backend/src/competition.mjs) | `BackendManagementActions.kt`:138 |
| `team.member.remove` | [competition.mjs:60](../backend/src/competition.mjs) | `BackendManagementActions.kt`:132 |
| `team.request` | [competition.mjs:55](../backend/src/competition.mjs) | `App.kt`:791 |
| `team.request.cancel` | [management.mjs:23](../backend/src/management.mjs) | `BackendEditingActions.kt`:100 |
| `team.request.review` | [competition.mjs:56](../backend/src/competition.mjs) | `App.kt`:806, `App.kt`:819 |
| `team.transfer` | [competition.mjs:62](../backend/src/competition.mjs) | `BackendManagementActions.kt`:142 |
| `team.update` | [management.mjs:24](../backend/src/management.mjs) | `BackendEditingActions.kt`:98 |
| `tournament.cancel` | [competition.mjs:68](../backend/src/competition.mjs) | `App.kt`:1018 |
| `tournament.complete` | [competition.mjs:69](../backend/src/competition.mjs) | `BackendManagementActions.kt`:237 |
| `tournament.create` | [competition.mjs:65](../backend/src/competition.mjs) | `App.kt`:998 |
| `tournament.register` | [competition.mjs:66](../backend/src/competition.mjs) | `App.kt`:1454 |
| `tournament.registration.remove` | [tournament-management.mjs:5](../backend/src/tournament-management.mjs) | `BackendManagementActions.kt`:267 |
| `tournament.registration.restore` | [tournament-management.mjs:16](../backend/src/tournament-management.mjs) | `BackendManagementActions.kt`:267 |
| `tournament.schedule` | [competition.mjs:70](../backend/src/competition.mjs) | `BackendManagementActions.kt`:238, `BackendManagementActions.kt`:250 |
| `tournament.withdraw` | [competition.mjs:67](../backend/src/competition.mjs) | `App.kt`:1454 |
| `user.block` | [social.mjs:16](../backend/src/social.mjs) | `App.kt`:1097, `App.kt`:1771, `BackendRepository.kt`:451, `AccountAndActivitySheets.kt`:82, `BackendManagementActions.kt`:36 |
| `venue.application.create` | [venue-applications.mjs:7](../backend/src/venue-applications.mjs) | `OperationsSheets.kt`:251 |
| `venue.application.withdraw` | [venue-applications.mjs:20](../backend/src/venue-applications.mjs) | `CommunityAndVenueScreen.kt`:153 |
| `venue.archive` | [operations.mjs:81](../backend/src/operations.mjs) | `OperationsSheets.kt`:179 |
| `venue.create` | [network.mjs:13](../backend/src/network.mjs) | `OperationsSheets.kt`:251 |
| `venue.review.delete` | [operations.mjs:143](../backend/src/operations.mjs) | `OperationsSheets.kt`:99, `VenueReviewSheets.kt`:86 |
| `venue.review.save` | [operations.mjs:131](../backend/src/operations.mjs) | `VenueReviewSheets.kt`:82 |
| `venue.slot.bulk.create` | [venue-schedule.mjs:9](../backend/src/venue-schedule.mjs) | `OperationsSheets.kt`:279 |
| `venue.slot.cancel` | [management.mjs:38](../backend/src/management.mjs) | `OperationsSheets.kt`:189 |
| `venue.slot.create` | [network.mjs:14](../backend/src/network.mjs) | `OperationsSheets.kt`:302 |
| `venue.slot.reopen` | [operations.mjs:107](../backend/src/operations.mjs) | `OperationsSheets.kt`:192 |
| `venue.slot.update` | [operations.mjs:97](../backend/src/operations.mjs) | `OperationsSheets.kt`:302 |
| `venue.update` | [operations.mjs:70](../backend/src/operations.mjs) | `OperationsSheets.kt`:251 |
| `verification.photo` | [identity.mjs:78](../backend/src/identity.mjs) | `App.kt`:311 |
