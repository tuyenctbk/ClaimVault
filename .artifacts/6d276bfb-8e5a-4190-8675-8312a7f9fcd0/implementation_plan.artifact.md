# Re-enable Sample Data for AVD Deployment

The user wants to revert the production-readiness changes that removed sample data. This is for demonstration purposes on the AVD.

## Proposed Changes

### Data Layer

#### [MODIFY] [WarrantyRepository.kt](file:///Users/user/antigravity/ClaimVault/app/src/main/java/com/example/data/repository/WarrantyRepository.kt)
Restore the `seedInitialDataIfEmpty()` function with the original list of sample electronics and home items.

#### [MODIFY] [ClaimVaultViewModel.kt](file:///Users/user/antigravity/ClaimVault/app/src/main/java/com/example/ui/ClaimVaultViewModel.kt)
Re-insert the call to `repository.seedInitialDataIfEmpty()` in the `init` block.

## Verification Plan

### Automated Tests
- Run `gradle_sync` to ensure project integrity.
- Build release APK.

### Manual Verification
- Deploy to AVD.
- Clear app data or fresh install to trigger the `seedInitialDataIfEmpty()` logic.
- Verify the home screen is populated with the sample Sony, iPhone, Dyson, etc., items.
