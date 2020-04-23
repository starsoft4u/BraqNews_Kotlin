package www.barq.news.custom

import com.beust.klaxon.TypeAdapter
import www.barq.news.model.Source
import kotlin.reflect.KClass

class JsonTypeAdapterForSource : TypeAdapter<Source> {
    override fun classFor(type: Any): KClass<out Source> {
        return Source::class
    }
}
