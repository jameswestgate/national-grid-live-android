# kotlinx.serialization — keep @Serializable metadata + generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.crainiate.nationalgridlive.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.crainiate.nationalgridlive.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
