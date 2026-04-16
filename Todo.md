For build : export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assembleDebug 2>&1 | tail -80

## Now
- [x] About the app screen
- [x] General settings — 24h/12h format, clickable URLs
- [x] Bulk "Start timer" in long-press → More
- [x] Project total tracked time (sum of task entries)
- [x] Daily alert — fixed times + day toggles (Mo–Su)
- [x] Logbook étendu — log every action in-app
- [x] Location-based reminders (offline GPS, named locations)
- [x] Integrate Location pop up auth to confirm using gps (foreground & background) precisely
- [x] Integrate Location-based reminders inside tasks properties

## May be
- [x] fix those deprecation warnings
- [x] cleanup pass
- [ ] Task property > choose/add location > gps (send reminder if i'm in location I put) : can store location and add label to it like home/studio ... (need to work full offline)
- [x] Statistiques : Dashboard, graphes, stockage utilisé

### Tablette
- [ ] **Master-detail ProjectDetail** — liste projets + détail côte à côte
- [ ] **Sidebar permanente** améliorée — hover states, tooltips
- [ ] **Task Detail panel latéral** — panneau glissant au lieu de navigation full-screen

### Sync & Compte (hors scope MVP)
- [ ] Authentification (compte local chiffré ou cloud)
- [ ] Sync backend (API REST ou Firebase)
- [ ] Export JSON / CSV
- [ ] Widget Android (résumé du jour)
- [ ] Raccourcis clavier tablette avec clavier physique