package eu.nk2.intercom.utils

internal data class NTuple2<T1, T2>(val t1: T1, val t2: T2)

internal data class NTuple3<T1, T2, T3>(val t1: T1, val t2: T2, val t3: T3)

internal data class NTuple4<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

internal data class NTuple5<T1, T2, T3, T4, T5>(val t1: T1, val t2: T2, val t3: T3, val t4: T4, val t5: T5)

internal data class NTuple6<T1, T2, T3, T4, T5, T6>(val t1: T1, val t2: T2, val t3: T3, val t4: T4, val t5: T5, val t6: T6)

internal infix fun <T1, T2> T1.then(t2: T2): NTuple2<T1, T2> = NTuple2(this, t2)

internal infix fun <T1, T2, T3> NTuple2<T1, T2>.then(t3: T3): NTuple3<T1, T2, T3> = NTuple3(this.t1, this.t2, t3)

internal infix fun <T1, T2, T3, T4> NTuple3<T1, T2, T3>.then(t4: T4): NTuple4<T1, T2, T3, T4> = NTuple4(this.t1, this.t2, this.t3, t4)

internal infix fun <T1, T2, T3, T4, T5> NTuple4<T1, T2, T3, T4>.then(t5: T5): NTuple5<T1, T2, T3, T4, T5> = NTuple5(this.t1, this.t2, this.t3, this.t4, t5)

internal infix fun <T1, T2, T3, T4, T5, T6> NTuple5<T1, T2, T3, T4, T5>.then(t6: T6): NTuple6<T1, T2, T3, T4, T5, T6> = NTuple6(this.t1, this.t2, this.t3, this.t4, this.t5, t6)