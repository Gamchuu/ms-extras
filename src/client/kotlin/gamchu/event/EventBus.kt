package gamchu.event

import gamchu.event.annotation.SubscribeEvent
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object EventBus {

    private val listeners = ConcurrentHashMap<Class<*>, List<ListenerData>>()
    private val registered = ConcurrentHashMap.newKeySet<Any>()
    private val dynamicRunnable = ConcurrentHashMap<Class<out Event>, MutableList<(Event) -> Unit>>()

    @JvmStatic
    fun register(obj: Any) {
        if (!registered.add(obj)) return

        obj::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(SubscribeEvent::class.java)) {
                val params = method.parameterTypes
                require(params.size == 1 && Event::class.java.isAssignableFrom(params[0])) {
                    "Invalid Method"
                }

                method.isAccessible = true
                val priority = method.getAnnotation(SubscribeEvent::class.java).priority
                val eventType = params[0]

                val consumer = createInvoker(obj, method)

                listeners.compute(eventType) { _, list ->
                    val newList = ArrayList(list ?: emptyList())
                    newList.add(ListenerData(obj, consumer, priority))
                    newList.sort()
                    Collections.unmodifiableList(newList)
                }
            }
        }
    }

    @Suppress("UNUSED")
    @JvmStatic
    fun unregister(obj: Any) {
        if (!registered.remove(obj)) return

        listeners.keys.forEach { key ->
            listeners.compute(key) { _, list ->
                val newList = ArrayList(list ?: return@compute null)
                if (newList.removeIf { it.instance === obj }) {
                    if (newList.isEmpty()) null else Collections.unmodifiableList(newList)
                } else {
                    list
                }
            }
        }
    }

    private val classCache =
        object : ClassValue<List<Class<*>>>() {
            override fun computeValue(type: Class<*>): List<Class<*>> {
                val classes = mutableSetOf<Class<*>>()
                var c: Class<*>? = type
                while (c != null) {
                    classes.add(c)
                    c.interfaces.forEach { classes.add(it) }
                    c = c.superclass
                }
                return classes.toList()
            }
        }

    @JvmStatic
    fun post(event: Event): Event {
        val eventClass = event::class.java

        classCache[eventClass].forEach { clazz ->
            listeners[clazz]?.forEach { data -> data.invoker.accept(event) }
        }

        handleDynamic(event)
        return event
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInvoker(instance: Any, method: Method): Consumer<Event> {
        return try {
            method.isAccessible = true

            val lookup = MethodHandles.privateLookupIn(method.declaringClass, MethodHandles.lookup())
            val methodHandle = lookup.unreflect(method)
            val boundHandle = methodHandle.bindTo(instance)

            val callSite =
                LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(Consumer::class.java),
                    MethodType.methodType(Void.TYPE, Any::class.java),
                    boundHandle,
                    MethodType.methodType(Void.TYPE, method.parameterTypes[0])
                )

            callSite.target.invokeExact() as Consumer<Event>
        } catch (_: Throwable) {
            Consumer { evt -> method.invoke(instance, evt) }
        }
    }

    private data class ListenerData(
        val instance: Any,
        val invoker: Consumer<Event>,
        val priority: Int,
    ) : Comparable<ListenerData> {
        override fun compareTo(other: ListenerData): Int {
            return other.priority.compareTo(this.priority)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Event> registerEvent(eventClass: Class<T>, listener: (T) -> Unit) {
        dynamicRunnable.computeIfAbsent(eventClass) { mutableListOf() }.add { event -> listener(event as T) }
    }

    @JvmStatic
    private fun handleDynamic(event: Event) {
        dynamicRunnable.filter { (clazz, _) -> clazz.isAssignableFrom(event::class.java) }
            .forEach { (_, listeners) ->
                listeners.forEach { it(event) }
            }
    }

}