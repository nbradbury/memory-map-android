# <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="48" height="48" align="absmiddle" alt=""> MemoryMap

An Android app that maps where your photos were taken. Places show up as stacks of polaroids on a sepia-toned National Geographic map. You can filter by date range and tap a place to flip through its photos.

Everything runs on the device. The app reads the GPS and capture time stored in each photo's EXIF data, and the only network traffic is map tiles and reverse geocoding.

<img width="300" alt="Screenshot_20261029_111218" src="https://github.com/user-attachments/assets/141f642b-6e3a-445b-9329-1aadf9ec0c1a" />

<img width="300" alt="Screenshot_20261029_114835" src="https://github.com/user-attachments/assets/ed05b7c0-3c6b-445b-8654-abadd4409750" />

<img width="300" alt="Screenshot_20261029_111322" src="https://github.com/user-attachments/assets/95423217-c1b6-4f79-8f53-1d1e4a8039bf" />


## Features

- Polaroid-stack pins with place names and photo counts. Nearby places merge into a single pin, and tapping it zooms in to split them apart.
- Date chips for This Month, Last Month, This Year, Last Year, and a custom month range.
- A custom range sheet: tap a start month, then an end month, in either order. Dots mark months that have photos.
- A carousel of polaroids for each place, with a thumbnail strip and a link that opens the photo in Google Photos (or your default gallery app).
- Trips that cross the Pacific (say, Tokyo to Honolulu) are framed across the antimeridian instead of around the globe.

## Building

Requirements:

- JDK 17 or newer
- Android SDK with platform 37 installed (compileSdk 37; targetSdk 36; minSdk 29)

Create `local.properties` with your SDK path, then build and install:

```sh
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew :app:installDebug
```

The first launch asks for photo access. Choose "Allow all"; with limited access the map only shows the photos you selected. The app also requests `ACCESS_MEDIA_LOCATION`, which has no dialog of its own. Without it, Android strips the GPS tags from EXIF data.

## How it works

1. `MediaStorePhotoSource` lists images from MediaStore and reads each one's GPS position and `DateTimeOriginal` from EXIF via `MediaStore.setRequireOriginal()`.
2. `IndexCache` saves the results to `files/photo-index.json`, so later launches only read EXIF from new or changed images.
3. `PlaceGrouper` buckets photos into 0.25° grid cells and reverse-geocodes each cell once with `Geocoder`. Cells that share a name become one place. If no geocoder is available, places are labelled with their coordinates.
4. The `domain` package does the rest in plain Kotlin, with no Android dependencies: it filters places to the selected date range, unwraps longitudes across the antimeridian, and clusters places by on-screen distance.
5. `ScrapbookMap` draws a MapLibre map using Esri's NatGeo raster tiles. The pins are Compose views placed over the map using the map's camera projection.

```
app/src/main/java/com/nbradbury/memorymap/
├── data/     MediaStore + EXIF scan, index cache, geocoding, permissions
├── domain/   Date ranges, filtering, clustering (pure Kotlin)
└── ui/       Compose screens, ViewModel, MapLibre map, theme
```

## Testing

```sh
./gradlew :app:testDebugUnitTest
```

The unit tests cover the date ranges and labels, date filtering, the antimeridian unwrap, clustering, place grouping, and the UI state the ViewModel derives.

To try the app on an emulator, push some geotagged photos to it. [exiftool](https://exiftool.org) can add GPS and dates to any JPEG:

```sh
exiftool -overwrite_original "-DateTimeOriginal=2026:06:08 10:15:00" \
  -GPSLatitude=38.7223 -GPSLatitudeRef=N -GPSLongitude=9.1393 -GPSLongitudeRef=W lisbon.jpg
adb push lisbon.jpg /sdcard/Pictures/
adb shell content call --method scan_volume --uri content://media --arg external_primary
```

## Screenshots without people

To take screenshots of your own library without featuring anyone, build with `-PhidePeople`:

```sh
./gradlew :app:installDebug -PhidePeople
```

Each pin then puts a photo with no people in it on top of its stack. A photo is skipped if ML Kit finds a face or MediaPipe's EfficientDet model finds a person, which catches people seen from behind. Only the top photo is checked, and pins appear once their check finishes. The code, the 4.6 MB model and both ML dependencies are only included in these builds (`app/src/hidePeople/`); normal builds use the no-op in `app/src/showPeople/`.

## Known limitations

- **Very wide zoom:** MapLibre on Android won't zoom out further than the point where the world fills the view's height. On a tall phone, a date range spanning two continents stops at about zoom 0.85, which puts pins at the edges close to the screen edge.
- **Place names:** names come from Android's `Geocoder` and use local spellings ("Lisboa", "Ciudad de México"). In big cities they're sometimes a district rather than the city.
- **Sepia map:** the map's sepia look is approximate. Raster styles can't do a true sepia, so the tiles are desaturated and a warm tint is drawn over them.

## Credits

- Original design: Claude Design's "MemoryMap Scrapbook" mock.
- Map tiles © Esri, National Geographic, Esri, DeLorme, NAVTEQ, UNEP-WCMC, USGS, NASA, ESA, METI, NRCAN, GEBCO, NOAA, iPC.
- Map rendering: [MapLibre Native](https://github.com/maplibre/maplibre-native).
- Font: [Figtree](https://github.com/erikdkennedy/figtree), SIL Open Font License.
