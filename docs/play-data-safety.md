# Google Play Data safety answers (draft)

Based on the actual behaviour of version 1.0 (verified from the merged manifest and code):

- No INTERNET permission. The app cannot transmit data off the device.
- No third party SDKs for analytics, ads or crash reporting.
- Android Auto Backup disabled (`allowBackup="false"`, data extraction rules exclude everything).
- Export and import only through the system file picker, started by the user. The app writes the file to the location the user chooses and does not send it anywhere.
- Permissions in the merged manifest: `VIBRATE`, `POST_NOTIFICATIONS` (rest timer), and `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (added automatically by AndroidX).

## Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | No |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data is collected) |
| Do you provide a way for users to request that their data is deleted? | Not applicable to collected data. In the app: Settings, Delete all data |

Reasoning: under Google Play's definitions, "collection" means data transmitted off the device. Active40 processes all data only on the device and has no network access, so nothing is collected or shared. User-initiated export to a file chosen by the user is not transmission by the developer.

## Related Play Console items to check before release

- Health apps declaration: Active40 records exercise and wellbeing information (and optional midlife symptoms). Complete the Health apps declaration form in Play Console and choose the categories that match (fitness and wellbeing tracking). Confirm the current form wording in Play Console at submission time.
- Privacy policy URL: publish `privacy-policy.md` as a web page and link it in the store listing.
- Target audience: adults.
- Ads: the app contains no ads.
