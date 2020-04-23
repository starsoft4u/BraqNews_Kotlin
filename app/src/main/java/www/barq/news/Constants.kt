package www.barq.news

class Constants {
    companion object {
        const val APP_KEY = "BarqNews"
        const val BASE_URL = "https://barq.news/BarqNewsApi/api"
//        const val BASE_URL = "http://192.168.0.102:88/BarqNews/src/BarqNewsApi/api"

        const val REQUEST_CODE_GOOGLE = 1010
        const val REQUEST_CODE_NOTIFICATION = 1011
        const val NOTIFICATION_CHANNEL_ID = "Barq News Last News"

        const val AD_UNIT_ID = "ca-app-pub-1137588610425183/1864314730"
        const val AD_OFFSET = 2
        const val AD_INTERVAL = 8

        const val ONE_WEEK = 604800000L
        const val ONE_DAY = 86400000L
        const val ONE_HOUR = 3600000L
        const val ONE_MINUTE = 60000L
    }
}