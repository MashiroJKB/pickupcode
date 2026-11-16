# Keep Room entities, DAOs, and constructors
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.pickupcode.data.** { *; }
-dontwarn androidx.room.paging.**
