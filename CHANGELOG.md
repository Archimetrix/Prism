# Changelog

What changed in each release. Older releases are described on the [Releases page](https://github.com/Yigder/Prism/releases).

## 1.7 (unreleased)

### New
- **Tablets, foldables and landscape**: on a wide screen the tabs move to a rail down the side, and the player shows the cover and controls beside the lyrics, the queue or the stream stats. On its side a phone gets the same player with a smaller cover. A foldable half-open like a laptop puts the cover above the fold and the controls below it. Settings lists its pages beside the one that's open, and albums and playlists keep their cover beside the songs on a tablet in landscape.
- **Jump to the best part**: in a song's first seconds a **Best part** button under the progress bar skips to the stretch listeners replay most (YouTube's own "Most replayed" data). Without that, the best part is where the chorus first comes round in the lyrics. The start lands on a lyric line or a bar, not mid-word. A song's menu has **Play from the best part** too. The button can be turned off in Settings → Player.
- **Headphone correction** (Settings → Playback & sound → Equalizer): pick your headphones from almost 9,000 measured by the AutoEq project, and Prism corrects them to sound neutral (the Harman target). The correction's curve is shown, with a strength slider. Each pair of headphones keeps its own correction and switches on when they connect; the phone's speaker and the car never get one. You can also import a ParametricEQ / EqualizerAPO file. It runs inside Prism, so it works on any phone and with Crossfade, and stats for nerds shows it.
- **Romanized lyrics**: songs in Korean, Japanese, Chinese, Arabic, Hindi, Russian, Greek and other scripts get each line in Latin letters under it (or in its place), so you can sing along. Karaoke lyrics light up word by word in Latin letters too. Korean (Revised Romanization, with its sound changes), Japanese, Russian and Greek are spelled out on the phone. Japanese kanji need a one-time 13 MB download of reading data ("kimi no na wa", not "kiminonaha"). Chinese (pinyin), Hindi, Arabic and the rest come from Google Translate, which is sent only the lyrics' text. Results are kept, so they work offline after the first time. Lyrics in your phone's own language are left as they are unless you ask (Settings → Lyrics → Romanize lyrics), and the player's ⋯ menu turns it on or off for one song.
- **Sing**: karaoke with the song's own music. The microphone button by the lyrics (or the player's ⋯ menu) takes the singer's voice out, worked out on your phone from the very copy that's playing (Deezer's Spleeter, a one-time ~50 MB download). A song is ready to sing a few seconds after you start, and a whole one takes about 30–40 s on a recent phone. The next song is prepared while this one plays. Leave a little of the lead voice in to sing along with, or none, and move the key up or down by up to six semitones (the tempo stays). The voice is only taken out where Prism has checked it lines up with the music to the sample, so nothing ever sounds doubled. Crossfade pauses while Sing is on. Prepared songs are kept (Settings → Lyrics → Sing: on mobile data or not, how much space to use, clear them). Separation is very good, but not perfect: echoes and some backing vocals can stay faintly.
- **DJ mode**: turn on **DJ** in the queue (or pick **Play as a DJ set** on an album or playlist), and Prism plays the songs ahead like a DJ would. Each song's key is found on your phone. Prism then orders the next 40 songs so each one goes into the next in a key that blends (the Camelot wheel), at a tempo close enough to beat-match, with no artist twice running. They're crossfaded and beat-matched: long blends where the keys sit together, short ones where they don't. Pick the energy: **Steady**, **Build up** or **Wind down**. The queue shows each song's key and tempo ("8A · 124"). Each song gets a quick listen (three 25-second stretches of it, a few songs at once, the next ones first), and the order improves as each one comes in; the playing song and the next one never move. Once a day, on Wi-Fi while your phone charges, the songs you play most and like are looked at ahead of time, so most sets start in order. On mobile data the queue says the songs are waiting for Wi-Fi, with a button to use mobile data instead. On 28 well-known songs the key was exact for 18 and within one step on the wheel for 26. Unclear keys aren't guessed. Shuffle turns DJ mode off, and a new queue starts without it.
- **Made for you**, worked out on your phone from what you play (nothing about your listening leaves it):
  - **Daily Mixes** (up to six) group your artists by what goes together, by genre and by which ones you play back to back. Each is 50 songs: ones you know, with new ones like them in between (two in every five). They're made once a day and stay the same all day, offline too.
  - **A time-of-day mix** ("Your Sunday evening rock") is made from what you play at this time of day, plus more like it.
  - **Release Radar** keeps an eye on the artists you've starred, saved and play most (looking every few days). It finds their new albums, singles and EPs with their exact release dates (from Deezer). They show on a **New releases** page and on Home, and in a weekly **Release Radar** playlist (refreshed on Fridays). There's a notification when a starred artist puts something out; turn it off in Settings → Home screen. The first look at an artist only learns what's already out, so their back catalogue is never announced as new.
  - The mixes are on Home ("Made for you", added once to existing layouts) and in Android Auto.
- **Smart Shuffle**: press shuffle again on a playlist or album, and a recommendation like the music around it slips in after every three or four songs (marked ✦ in the queue). While one plays, the ✦ button by the like button adds it to the playlist (your own YouTube playlists) or turns it down. Turning Smart Shuffle off takes the ones still to come back out.
- **Exclusions**: "Don't play this artist" (artist page ⋯) keeps an artist out of radios, autoplay, Smart Shuffle, your mixes and Smart Downloads. "Leave out of my taste" (a playlist's ⋯, a song's menu) still plays them, but they no longer shape your recommendations, search, or Smart Downloads; Replay counts them as usual. Everything left out is listed in Settings → Library & Replay → Exclusions.
- **Blend** (Library → Blend): a playlist of two tastes. Show a friend your QR code (they scan it in Prism), or send it as a message they share to Prism, or paste it. Prism shows your taste match, the artists and genres you share, and a 50-song playlist: the songs you both play first, then each of yours in turns. Both phones make exactly the same Blend once you've each taken the other's code. There's also "Discover together" (new to you both, like what you share), Play / Shuffle, Save to YouTube Music, and a picture to share. No server is involved: the code carries your Blend name, top artists and songs, and genres, and you can see exactly what's in it ("What you share"). Artists you don't play and things left out of your taste aren't in it.
- **Song recognition**: tap the new button in Search's box (or add the **Recognize song** tile to Quick Settings) and Prism listens for up to 12 seconds to tell you what's playing around you. It then finds the song on YouTube Music, ready to play, like, or open its album or lyrics. The sound stays on your phone: Prism makes Shazam's fingerprint of it on the phone (ported from SongRec) and only that goes to Shazam, with no location. If Prism is playing, it pauses while listening (or keep it playing to recognize Prism's own song). Offline, the fingerprint is kept and looked up when you're back online, with a notification when it's found. Everything found is in Library → Recognized. The microphone is asked for the first time you use it, and used only while that screen is open.
- **Prism Connect** (Settings → Prism Connect):
  - **Speakers & TVs**: the player's device list (the line under the controls) now finds speakers, TVs and AV receivers on your Wi-Fi that play music sent to them (DLNA / UPnP). Pick one and the music plays there; the queue, lyrics, notification and Android Auto carry on as usual, the phone's volume keys and the player's volume bar set its volume, and pausing or skipping on the TV's own remote comes back to Prism. Songs go to it decoded on your phone into plain uncompressed audio, which every renderer plays (a renderer that refuses one kind gets the other), and run into each other without a gap where it allows. The equalizer, Crossfade, Sing and videos are the phone's own, so they're off while casting, and live streams play on the phone. No Google Cast: Chromecast-only speakers don't show.
  - **Control from a browser**: switch it on and open the address shown on any computer or tablet on the same Wi-Fi. After typing a six-digit code from the phone (once per browser), it shows what's playing, its controls and volume, the queue, search, and lyrics that light up word by word. Only browsers you pair get in, guessing the code is locked out after a few tries, and nothing goes through the internet. It works while Prism is open or playing.
  - **Handoff**: move what's playing to another phone or tablet with Prism (the same Wi-Fi, "Available for handoff" on there), or bring theirs over: the whole queue, the song and place, shuffle and repeat. The first time, the other device shows a code to type in; after that they know each other. Songs that are files on one device stay behind. Prism has to be open (or already playing) on the device the music moves to, as Android doesn't let an app start playing from the background; if it isn't, you're told so and the music keeps playing where it was.
- **Rewind**, every year: Replay now has a chip for each year you've listened in (not only this year and last), and each year's story is its **Rewind**. This year's opens on December 1 (with a notification, which you can turn off in Settings → Library & Replay, and a Rewind card on Home through December); earlier years any time. Besides your minutes, genres, top artists and songs, habits, discoveries and personality, it has your year season by season, your top album, your biggest month, the first song you played that year, your year in Prism (songs sung, DJ sets, Smart Shuffle finds, songs recognized, Blends, new releases found) and how the year compares with the one before. Your #1 song shows a line from its best part, and every song in the story plays from its best part. A quiet year gets a short story rather than empty slides. Any slide can be shared as a picture, and "Save Top 100" makes a YouTube Music playlist of the year. Once a year is over, its Rewind is kept as it was (in your backups too), so it opens at once.
- **Playback diagnostics** (Settings → About): if music stutters, switch this on and Prism keeps a log on your phone of what the audio does: the output being set up, moments it ran dry, Bluetooth and other devices coming and going, how long Prism's own sound processing takes, and the background work running beside the music. Tap **Mark a stutter** in the player's ⋯ menu when you hear one, then **Share diagnostics** to send the log (with your phone's model and Prism's audio settings) to whoever is helping. Nothing is sent unless you share it.
- **Recognize widget** (Prism · Recognize in your launcher's widget list, or Library → Recognized → Add the widget): a round button on your home screen that opens a small listening card over whatever's on screen, without opening Prism. Made wider it also shows the last song recognized with a Play button (one row at 4×1, a card at 4×2), or that one is saved to look up once you're online. The **Recognize song** Quick Settings tile now opens the same card instead of the whole app. The microphone is only used while the card is on screen; leaving it stops listening.
- **Pick artists you like**: after the welcome screen (and any time from Settings → Home screen → Your artists), choose from YouTube Music's genres or search. They're starred on your phone (nothing is followed on YouTube), so your mixes and Release Radar have something to go on from the first day.

### Fixed
- **YouTube Music's own mixes all showed the same cover** (My Supermix's). Prism keeps covers on the phone so they show offline, and it filed every mix's cover under one name, since their addresses differ only after the "?". Each mix now keeps its own, and the shared one is cleared on the first start.
- **Replay, Rewind, Blend and the mixes counted a song's whole credit as one artist** ("The Kid LAROI, Justin Bieber" as an artist of its own). A song now counts for its main artist. Songs are also stored with their exact artists, so a name with a comma in it ("Tyler, The Creator") stays whole.
- **Music files without an artist tag counted as an artist called "Unknown artist"**, with a photo of whoever YouTube has by that name, and their genre was guessed from the title alone (so a few test tones became "Dance & electronic"). They still play and show in Replay's songs, but no longer count as an artist or a genre. Wrong guesses already made are cleared on the first start.
- **Some songs played as AAC where YouTube Music plays Opus** ("Espresso", "Levitating", "Mr. Brightside" and others). YouTube's stated bitrate for the AAC stream is sometimes a hair higher than the Opus one, and Prism took the bigger number. Opus is the better codec at the same bitrate, so Prism now picks it first at every quality setting, as YouTube Music does (256 kbps AAC for Music Premium still beats 128 kbps Opus).
- **Some songs had no music video though they have one**, like "Linger (SiriusXM Session)" by Royel Otis, whose video is on SiriusXM's channel. Prism only took a video from the artist's own channel and never a "live" or "cover" one. A video from another channel now counts when it's the very same recording: the same length to within a few seconds, naming the artist, and from the same source the song names (SiriusXM here). Lyric videos, karaoke, fan edits and audio-only uploads still never count. A song found without a video is looked up again after an hour, not only after a restart.
- **A music video that YouTube refused to serve once fell straight back to the song.** It now gets one more try with fresh links (the other way, through NewPipe) first.
- **TalkBack:** with the full player open, TalkBack could still move to the screen, tabs and mini player hidden under it; they're now left out until the player closes. Immersive mode said nothing at all: it now reads the song and its lyrics, a double tap opens its settings, and its actions menu has Next song, Previous song, Leave immersive mode and Close the player (the swipes and the hold aren't possible with TalkBack). Its settings button is a proper button, with Leave immersive mode as an action.
- **An album or playlist page's ⋯** was a short dropdown without Play next, Add to queue or Play as a DJ set. It's now the same menu as a long press on the album, with the page's own actions (radio, library, downloads, picture, edit, delete) in a group under it.
- **Settings search** could open a setting that's hidden until another is on (Crossfade's Length needs Crossfade on). Those results now say what to turn on first, and open at that switch.
- **Library → Downloads** counted downloads waiting for Wi-Fi as "in progress"; only ones actually downloading count now.
- **A WAV file's quality** read "AUDIO/RAW 705 kbps" on the player; lossless files now show their format and resolution ("WAV · 16-bit · 44.1 kHz", "FLAC · 96 kHz").
- **Sing's download size** said 51 MB in one place and 76 MB in another; both now say how big the download is and how much room it takes on the phone.
- **Settings tidied:** Discord has its own page instead of the top of Settings; Local files has its title under Back like the other pages; the welcome screen keeps a phone's width on tablets; the album caption above the cover reads "From the album …" (it's the song's album, not necessarily what's playing); and Back leaves immersive mode for the normal player instead of closing it.
- **Search:** with little listening yet, "For you" and "Moods & moments" showed the same tiles (the second list now leaves out what the first shows); the Top result card has the same long-press menu as the results under it; and a hardware keyboard's Enter searches like the on-screen key.
- **"See all" grids of many artists' releases** (like YouTube's own New releases) said only "Album" or "Single" on each card; they now name the artist.
- **An album's "Other versions"** could show three identical "After Hours (Deluxe)" cards; editions with the same name now show their song count, year and whether they're explicit.
- **Two "New releases" on Home:** Prism's own section (from Release Radar) is now called **New from your artists**, so it isn't confused with YouTube's shelf.
- **Sing's engine (ONNX Runtime) has its own usage reporting, now switched off outright.** It could never start on Android (it has no way to send), so nothing was ever sent, but Prism doesn't leave that to chance.
- **"Pick artists you like" (first start):** the Done button sat under the tab bar, which now stays hidden until setup is finished; the genre chips all showed the same hip-hop artists (YouTube's genres share one page address, and Prism told them apart by that alone), and genres whose pages list only albums and playlists (Rock, Pop) came up empty. Each genre now shows its own artists, with their photos (some big names, like Drake, showed blank).
- **Bluetooth earbuds (AirPods especially) could make the music skip or judder.** Earbuds register and drop several audio devices as they work (their microphone, call channels, ear detection, the case), and each one made Prism re-point its audio output, even when nothing had changed; on Bluetooth each re-route is a short gap. Prism now only lets go of an output you picked, and only for a real new output. Headphone correction waits for the devices to settle before switching. Sing's processing no longer runs on every sample of music when Sing is off, and the song scans behind Normalize, Crossfade and DJ mode wait while the playing song is still loading.
- **DJ mode took minutes to get a set in order**, reading every song whole, one at a time. It now hears three stretches of each song, three songs at once (the key matched the whole song's for 26 of 28 test songs, and the tempo for 27). On mobile data it used to say "in key order" while it couldn't read a single key; it now says what it's doing ("reading keys… 4 of 24", "reading keys… 0 of 1, 41 wait for Wi-Fi" with a **Use mobile data** button, "10 of 24 songs in key order").
- **Daily Mixes could skip a number** ("Daily Mix 1", "Daily Mix 3") when a group of artists didn't have enough songs for a mix.

## 1.6.9.2

### Fixed
- **Discord sign-in stayed on Discord's page after signing in.** Prism now picks up the session from Discord's own sign-in answer (password, two-factor or QR code) and its requests, checks on every page rather than only once the address changes, and finds Discord's session wherever its current code keeps it.

## 1.6.9.1

### Fixed
- **Discord sign-in showed a blank page** when Discord's page couldn't load (no connection, or a network that blocks Discord, as many work and school ones do). It now says it couldn't reach Discord, with Try again.
- **Discord sign-in** looks like the phone's browser to Discord (Android's in-app browser marked itself), and reads the sign-in a second way if the first doesn't find it.

## 1.6.9

### New
- **More than one account**: Settings → "Switch account" lists every YouTube channel you can use, including several channels (brand accounts) under one email, and "Add a Google account" signs another one in beside it. Switching brings that channel's library, likes and recommendations; Replay and this phone's history stay.
- **Discord** (Settings → Discord): "Listening to Prism" on your Discord profile, with the song, artist and how far in, cleared when you pause. Off until you sign in to Discord on its own page; the sign-in stays on this phone and never goes in a backup. Discord has no official way for Android apps to do this, so it works the way other Android music players do, which Discord's terms don't allow for third-party apps: there's a small risk to your Discord account.
- **Cupertino icons** (Settings → Appearance → Icons → Pack): the most-seen icons redrawn in the style of Apple's apps (filled tabs, a chevron for Back, two-triangle skips, circled menus and downloads).
- **Remove from library** in an album's long-press menu.

### Fixed
- **Lyrics stuck on the last line** from the start of a song ("4 Steps Back, 2 Steps Forward" by Motion Sick): one source sent plain lyrics marked as synced, every line at 0:00. Those now count as plain, so a source with real timings wins.
- **Quick picks popping in and out**, and Home showing the same songs twice: Quick picks and Speed dial no longer reload each time they scroll off screen, a refresh without Quick picks keeps the last ones, and a section already shown isn't repeated.
- **Slow starts and buffering since 1.6.8**: saving playlists, albums and covers for offline, and retrying failed downloads, now wait while music is loading, and do less at a time.
- **A crash** when Prism started in the background with failed downloads waiting (Android 12 and newer).
- **Albums that wouldn't leave the library** when YouTube opened them as another edition (a remaster or deluxe).

## 1.6.8

### New
- **Speed dial** on Home (Customize Home → Add a section), as on YouTube Music's app: your go-to songs, albums, playlists and artists as big tiles, nine to a page. YouTube's own Speed dial (or Listen again) leads, filled out with what you play most. It goes in at the top (#53).
- **Quick picks start a radio**: tapping a pick plays it and then songs like it, as on YouTube Music. Customize Home → Options turns it back to playing Quick picks in order (#38).
- **Search your own music**: Search has YouTube Music and **On this phone**, which finds your music files and downloads by title, artist or album. Offline, Search starts there (#46).
- **Offline library**: your playlists, albums and artists are kept as last synced, and playlists and albums are saved with their songs when opened (and on Wi-Fi after a sync), so they open with no connection. Offline, a playlist plays its downloaded songs and shows the rest greyed out (#55).
- **Covers offline**: downloaded songs' covers (a lossless file's own embedded cover first) and your playlists' and albums' are kept on the phone, for the player and its blurred background, the notification, widgets and lists. Local files whose album has no artwork in Android show the cover inside the file (#54, #55).
- **Data saver** (Settings → Playback & sound → Mobile data): on mobile data, pictures come at the size they're shown, artist photos and animated covers wait for Wi-Fi, songs stream at your mobile data quality (at most Normal) and music videos at 480p at most (#40).
- **Songs, not videos** (Settings → Playback, on by default): autoplay and radios play music videos as their songs and leave out videos with no song version (#39).
- **Stop when swiped away** (Settings → Playback): swiping Prism out of your recent apps stops the music (#45).
- **Choose your lyrics sources**: each source in Settings → Lyrics → Sources has a switch and a line on what it's good for; Prism only asks the ones switched on (#51).
- **Save lyrics into your music files** (Settings → Lyrics, or a song's ⋯ menu): synced lyrics go into your FLAC and MP3 files' tags as LRC, so other players show them too (#16).

  > **About #16:** Prism has always left your own music files alone, so this only happens when you ask for it: nothing is written until you tap it in Settings → Lyrics or a song's menu, and on Android 11 and newer Android asks you to allow the change first. Only the lyrics tag is written; the audio and every other tag stay as they were. Each file is copied first and checked before it goes back (if anything goes wrong, even Prism closing mid-way, the original is put back). Prism still never deletes or moves your files.
- **New widgets**: Cover (2×2), Disc (2×2, with a progress ring) and Strip (4×1) (#34).
- **Local files sorting**: a sort sheet with the direction in plain words and more orders (date added, year, length, file size), plus orders for Artists and Albums (#35).
- **Every artist of a song**: a song's menu has "Go to …" for each of its artists, and tapping the artist on the player asks which one (#42).

### Fixed
- **Downloads stuck at a percentage** (#55): failed downloads now try again by themselves at launch and when you're back online, with fresh links; one that stalls restarts from where it got to; and Downloads says when they're waiting for Wi-Fi or failed, with "Use mobile data" and "Try again". Offline, songs that aren't fully on the phone are passed over instead of cutting off mid-song.
- **Music videos with the Song / Video switch off** (#48): a music video picked from a list plays as its song, and nothing switches to video.
- **Headphone volume lag** (#52): the player's volume slider only sends real changes, so Bluetooth headphones no longer fall seconds behind it.
- **Grid** for Quick picks and other song sections shows covers in two rows; it looked the same as List (#44).
- **Artist pages** (#47): the photo's colours carry on down the page and settle into its own tint instead of fading to black (album and playlist pages too). The artist's name moves with the photo as you scroll (#37).
- **The player's title** is a size larger than the artist under it; they were both 20 sp (#41).

## 1.6.7

### New
- **Your own fonts** (Settings → Appearance → Text): pick a .ttf or .otf file from your phone (Inter, SF Pro…) for the whole app, and choose a separate **lyrics font**: the app's, any built-in font, or a file of its own. Variable fonts get all their weights (#12).
- **Icon style** (Settings → Appearance → Icons): Thin, Regular or Bold icons, rounded or square. Applies after a quick restart, from the button that appears (#19).
- **App icons** (Settings → Appearance → App icon): Classic, Spectrum, Solid, Light, Wave and Midnight (#20).
- **Local files, reworked** (#29): Play, Shuffle and **Recently played** at the top, the song and album count under the title, songs sorted by title by default (the order you pick is kept), and new sections: **Favourites** (liked songs from your phone), **Recently added**, **Added albums** and **Downloads**. Tap **Edit** at the end of the bar to choose and order the sections.
- **Hide the Podcasts button** on Home (Settings → Home screen → Podcasts button); Podcasts then opens from Library (#28).

### Fixed
- **Lossless FLAC at double speed with harsh noise** on Samsung phones (#25): Samsung's FLAC decoder mishandled 16-bit (CD-quality) files in lossless mode. They're now decoded at 16-bit, which loses nothing; 24-bit files stay hi-res.
- **Searches showing up in your YouTube search history** (#27): Prism's searches, every keystroke and the lookups it makes for songs, were saved to your YouTube account. Searches no longer go out with your account.
- **Podcasts "Requested entity was not found"** (#18): in countries where YouTube Music has no Podcasts page, Prism shows your followed shows and saved episodes, and points to Search, instead of an error.
- **Phones in other languages** (Korean, Spanish and more): YouTube answers in your phone's language, and Prism misread its words for "Song", "Single", "Album", years ("2026년") and listener counts, which mixed up artists, album types and search ranking. They're understood in 20 languages now. The date on Home follows your language's order. (Prism's own screens are still in English.)

## 1.6.6

### New
- **Hold previous / next to seek** (Settings → Player → Hold to seek, on by default): holding jumps back or forward, again and again while you hold. Pick 1–20 seconds per step, or turn it off. A tap still changes songs (#21).
- **Local files as a tab** (Settings → Layout & navigation → Tabs): put Local files in the bottom bar, and open Prism on it if you like (#17).
- **Song / Video switch** (Settings → Player → On the player): turn it off to keep the player audio only (#22).
- **"Hey Google, play … on Prism"**: Google Assistant and Gemini can now pick Prism as the app to play from. "Play music on Prism" carries on with your last queue (#23).

### Fixed
- **Right-to-left lyrics on the player:** the arrow after an Arabic line now sits on its left, at the end of the line, and points left (#14).
- **Podcasts** showed "Requested entity was not found" for some signed-in accounts. Prism now loads the page as a guest when YouTube refuses it for the account (#18).
- **Closing the player:** swiping it down and pressing Back now slide it away the same way (#26).

## 1.6.5

### New
- **Immersive mode** (the player's ⋯ menu, or Settings → Player): the cover fills the whole screen, its animated version first when it has one, and the lyrics play over its lower part a line at a time, like a lyric video. Everything else hides; tap the screen for a settings button at the bottom (back to the normal player, lyrics timing and source, the song's options), or **hold it** until its ring fills to go straight back to the normal player. Swipe left or right for the next or previous song, and drag down to close the player. The cover can fill the screen, be **zoomed out** (the default) or show **whole**, over a blurred copy of itself (the settings button, or Settings → Player). The lyrics can sit **high**, in the **middle** or **low** (the default) on the screen, or run **over the whole screen**: every line, the one being sung large and bright, the list gliding along by itself.
- **Crossfade** (Settings → Playback & sound, formerly Mix) now works like Spotify's: the ending song fades out while the next one fades in, both at their own speed. **Automatic length** (on by default) works out each crossfade from how the songs end and begin: over a song's own fade-out, over the next song's quiet intro after an abrupt ending, and only briefly between two hard edges. Or set 1–12 seconds yourself. Matching tempo and beats is now an option (DJ-style), off unless you turn it on. Songs already measured are measured once more for their fade-outs and intros.
- **Local files** has its own page (Library → Playlists → Local files), with **Songs**, **Artists** and **Albums**. Artists open to their songs album by album, and albums play in track order.
- **Shuffle and repeat on the player** (Settings → Player → Shuffle and repeat buttons): either side of play, always. Repeat goes all, one, off.
- **Load ahead** (Settings → Playback & sound): besides the next song's first seconds (as before), the next song or the next three can be loaded whole while one plays, so they play without buffering even if the signal drops. On Wi-Fi, or on mobile data too if you allow it (for the next song's start as well).
- **Swipe a song** in an album or playlist (Liked songs, Downloads and Local files too): right to **play it next**, left to **add it to the end of the queue**.
- **Android Auto lyrics: Keep the song title** (Settings → Lyrics → Android Auto, with the Karaoke or Line by line style): the lyrics show only on the cover, so the car's small media card reads as usual and its full player shows the lyric card big.
- **Settings tidied:** the Player page has separate Artwork, Immersive mode and Animated covers groups, and "On the player" lists things top to bottom as they sit on the player ("Hide volume bar" is now "Volume bar"). Lyrics sources and how they're picked share one group. The same bar is called the progress bar everywhere.

### Fixed
- **The player sits higher again,** as before 1.6.4: the cover and the play, back and forward buttons had moved down. The lyrics, Song/Video and queue buttons along the bottom stay where they are, and the title still sits close to the progress bar.
- **Quick picks** showed your recent plays again: YouTube's "Listen again" / "Speed dial" shelf was taken for them. When YouTube has no picks, songs like your latest play stand in, never the songs you just played.
- **Animated covers:** after saving one song's animated cover, the album's other songs showed their still cover. Looking up covers for saved albums and downloads no longer holds up the cover of the song that's playing.
- **Songs start faster with Crossfade or Normalize volume on.** Measuring a song (which can mean fetching and decoding all of it) ran just as a song started, slowing its loading. Now the next three songs in the queue are measured (and their loudness looked up) while the current one plays, once it's under way, at low priority.
- **Normalize volume no longer jumps a few seconds into a song.** A song's volume is set as it starts; a loudness figure that only arrives later is kept for next time instead of changing the volume mid-song. Queued songs have theirs ready before they play.
- **Animated covers keep playing** while the ⋯ menu or another sheet is open over the player, instead of switching to the still cover.
- **Shuffle:** Previous went to a song that hadn't played yet. It now goes back to the song played last, including after songs are added to the queue or a song is picked from it, and Play next plays next while shuffled too.

## 1.6.4

### New
- **Cover size** (Settings → Player → Artwork): Standard, **Large** (narrow side margins, like YouTube Music) or **Full width** (the square cover runs edge to edge). With Large or Full width, a hidden volume bar or output device gives its room to the cover (#10).
- **Stream quality** (Settings → Player, where "Stats for nerds" was) shows or hides the codec and bitrate (or "Downloaded") under the progress bar. The stats line over the cover is gone; tap the label, or use the ⋯ menu, for the full stats (#8).
- **Animated covers are saved with downloads**, so downloaded songs keep their moving cover offline. "Save with downloads on mobile data" decides whether that waits for Wi-Fi. An album's songs share one saved copy when they use the album's cover.
- **Quick picks snap** a page of rows at a time when swiped, like YouTube Music (#11).

### Fixed
- **Arabic and other right-to-left lyrics** sit on the right and read right to left, and word-by-word lyrics light up from the right (#7). The line on the player is right-aligned too.
- **Time-synced lyrics from YouTube Music** (LyricFind) are used when a song has them, so songs that only had plain lyrics there now scroll in time, Arabic songs among them (#6).
- **With "Current lyric" off,** the player no longer keeps a blank line for it, and the title sits just above the controls on tall screens (#9).
- **Quick picks** showed your recent plays instead of YouTube's picks when YouTube was in another language (#11).
- **Android Auto's queue** (and the watch's and notification's) follows the shuffled order you hear, and tapping a song in it plays that song.

## 1.6.3

### New
- **Your music files.** Library → **On this phone** lists the music on your phone (MP3, M4A, FLAC, OGG, WAV…) with its own tags and album art, sorted by date added, title, artist or album, with Play and Shuffle. Songs play from the file, with normalization, EQ, Mix, lyrics and Replay. Prism asks once for access and only reads the files.
- **Smart Downloads is one storage space.** It now covers both the songs you stream (the old song cache) and the songs it downloads ahead, under one size (your old two sizes added together). When it's full, the songs you're least likely to play again make room, and liked songs are kept longest. **Smart Downloads on mobile data** lets it download without Wi-Fi, and **Clear Smart Downloads** empties it (songs you downloaded yourself stay).
- **Prism's own icons** everywhere: minimal, rounded line icons in place of Google's.
- **Search:** better top results. Abbreviations work ("adtr" finds A Day to Remember, also "rhcp", "mcr"), YouTube's own top pick is used unless it's a cover or remix you didn't ask for, a song wins over its music video, and suggestions start with your artists and recent searches.
- **Replay** lists your top 100 songs.
- Settings → About → tapping the version opens Prism's GitHub page.

### Fixed
- **Normalize volume** really evens songs out now: each song's loudness is measured on your phone (LUFS, as Spotify and YouTube use) and brought to the same level. YouTube's own figure, used before, hardly differed between loud and quiet songs. The next song's level is ready before it starts. Stats for nerds shows the measured loudness.
- **Mix** blended into silence, or not at all, on songs whose music ends before the file does (a fade-out or a silent tail): blends are now timed from where the music ends, the next song comes straight in if the ending one goes quiet, silence at the start of the next song is skipped, and a slow-loading ending gets more time. "Measure songs on mobile data" (Settings → Playback & sound → Sound) covers Mix and normalization; songs already on your phone are measured without data.
- **Scrubbing** after changing songs could jump too far (it used the first song's length).
- **The player's ⋯ menu** stuck or stuttered when pulled down on songs with animated covers.
- **"Alright" (Kendrick Lamar)** and other long explicit songs froze the player for a moment as they opened, while starred-out words were filled in; songs with long music videos also opened sluggishly.
- **Music videos with a long intro** (like "Alright") have their lyrics lined up again.
- **After a podcast,** music plays as the song again, not its video.
- **Live streams** load again when signed in.
- **Home's frosted background** follows the song that's playing (#3).
- **Search:** the search box has its background back, and a top result's play button turns into pause (#5).
- **Replay:** genre names are readable with a white accent in light mode.

## 1.6.2

Everything since 1.6.0 (1.6.1 was tagged but never released).

### New
- **Podcasts.** The round podcasts button at the top of Home, beside Customize, opens them.
  - **Customize button at the top** (Settings → Home screen) can be turned off; Podcasts then takes its place.
  - **Continue listening:** episodes you've started pick up where you left off.
  - **Your shows** and **Episodes for later** from your YouTube Music account, when signed in.
  - **Popular shows and episodes**, by category.
  - **Show pages:** newest episodes first, more as you go, save to library and share.
  - **Search:** new Podcasts and Episodes filters.
  - **Speed:** 0.75× to 2× from the player's ⋯ menu, for episodes only.
  - Episodes don't look up lyrics, don't blend with Mix, aren't followed by music radio and don't count in Replay.
- **Home screen widgets**, in four looks: Prism (frosted cover), Spotify style, Apple Music style and YouTube Music style. Each shows the cover, title, progress, previous/play/next and like; tapping the cover opens the player.
- **Content country** (Settings → Home screen → Region). Where YouTube Music isn't offered, pick a country where it is to get Home, charts and search back. Songs a label blocks in your country still won't play.
- **Mix on mobile data.** "Measure tempo on mobile data" lets beat matching work away from Wi-Fi (about 1–4 MB per song).

### Fixed
- **Pixel Watch:** previous and next sit beside play again, instead of like and shuffle.
- **Lyric timing:**
  - lyric files that shift their own timing (an `[offset]` line) were read without the shift, so every line was early or late;
  - some sources used an extended or radio edit's lyrics, which ran early or late; versions more than 8 seconds off are now turned down, and lyrics saved from those sources are checked again once;
  - while paused, lyrics no longer creep ahead and jump back.
- **Karaoke lyrics first.** Lyrics that light up word by word are now called **Karaoke** (Lyrics source shows "Karaoke", "Time-synced lines" or "Plain text"). They win over time-synced lines even when a source earlier in your order only has lines, and songs whose time-synced lyrics were saved before look for karaoke again.
- **Uncensored lyrics:** words starred out whole (`****`, as Apple Music's lyrics do) are filled in now, as in Childish Gambino's "IV. Sweatpants". Songs with a guest in the title find their full words again, and YouTube Music's or LRCLIB's lyrics fill in when Genius doesn't have the song.
- **Menus:** swiping a menu down (like the player's ⋯) could stutter, or pop back up after being flung away.
- **Player:** an interrupted swipe down could leave the player stuck part of the way down.

## 1.6.0

### New
- **Mix.** Songs blend into each other instead of stopping and starting (Settings → Playback & sound → Mix, off by default).
  - **Blend length:** 2 to 12 seconds.
  - **Match tempo and beats:** Prism measures each song's tempo on the phone, on Wi-Fi. The next song comes in on the beat and plays at the outgoing song's tempo while they overlap (pitch kept), then eases back to its own.
  - **No blend for:** an album's own tracks, live streams, music videos and Listen Together. These still play as before.
- **Translated lyrics.** A translation can show under each lyric line when a song is in another language.
  - **Where:** Settings → Lyrics, or the player's ⋯ menu.
  - **Language:** your phone's language by default, or one of 20 others.
  - **Privacy:** this is a machine translation, and the lyrics' text is sent to Google Translate.
- **"Next in queue".** Songs you add with Play next or Add to queue get their own heading in the queue. Add to queue now puts songs before autoplay's suggestions instead of after them.

### Faster
- **Songs start sooner.** Prism gets the likeliest next song ready ahead of time and, on Wi-Fi, saves its first few seconds. This covers:
  - the top search result;
  - the first song of an open album or playlist;
  - the next song in the queue, including after the queue changes or shuffle is turned on;
  - the song in the mini player when the app opens, and the one after it.
- **Results:** a song that was got ready starts in about half a second instead of over two seconds.

### Fixed
- **Back gesture animation:** the phone's back button or gesture now fades between pages like the back arrow does, instead of shrinking the page like a card.
- **Plain error messages:**
  - "You're offline", instead of technical text;
  - searching with no connection says so, instead of "Search failed";
  - a clear message when YouTube is limiting playback on the network.
- **Refused requests after signing in:** YouTube refused some requests ("Request contains an invalid argument") for some accounts and regions, such as brand accounts and es-419. Prism now retries them in a plainer form.
- **Blocked networks:** when YouTube blocks anonymous playback on a network, signed-in playback goes straight to the account's own clients for a while, instead of failing on each song first.
- **Bitrate:** the player shows each song's real average bitrate. It used to show YouTube's nominal figure (Opus always said 160 kbps).
- **Album tracks:** signed out, some albums' tracks opened as music videos. They now play as songs.
- **Switching between song and video** no longer counts as a new play in history and Replay.
- **Lyrics and queue scrolling:** the player controls no longer flicker in and out while you scroll lyrics or the queue (seen on 120 Hz phones).
- **Home:** a stale "Quick picks" row no longer flashes up while Home loads.
- **"See all" pages:** mixes and playlists now show as a two-column grid of covers, not one sideways row.
- **Fonts:** Google Sans and Rounded are greyed out, with a note, on phones that don't have Google Sans Flex. Picking them there used to do nothing.
- **Counts:** they read "1 song" and "1 artist", not "1 songs".
