package com.example.rikharthu.robotcar.events

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxEvents {

    private val events = PublishSubject.create<IRobotEvent>()

    fun all() = events

    fun <T : IRobotEvent> of(clazz: Class<T>): Observable<T> {
        return events.filter(clazz::isInstance).cast(clazz)
    }

    fun post(event: IRobotEvent) {
        events.onNext(event)
    }
}