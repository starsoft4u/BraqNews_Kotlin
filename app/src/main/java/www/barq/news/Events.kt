package www.barq.news

class Events {
    class SelectTab(val index: Int)
    class LoggedIn
    class SignedUp
    class LoggedOut
    class Favorited(val newsId: Long, val favorited: Boolean)
    class Commented(val newsId: Long, val count: Int)
    class Followed(val sourceId: Int, val follow: Boolean)
    class SourceModified
}