package provingground.library

import provingground._, HoTT._
import Nats.{Nat => NatTyp, _}
import NatInd.{rec, induc}
// import spire.implicits._


object NatsDecEq{
    private val n = "n" :: NatTyp
    private val m = "m" :: NatTyp
    private val k = "k" :: NatTyp

    val recNatType = rec(Type)
    val recNatNatType = rec(NatTyp ->: Type)

    val A = Type.sym

    val step01 = n :-> (A :-> (Zero: Typ[Term]))

    val base0 = recNatType(One)(step01)

    val Q = (NatTyp ->: Type).sym


    val step0value = recNatType(Zero)(k :-> (A :-> Q(k)))

    val step0 = n :-> (Q :-> step0value)

    val AreEqual = recNatNatType(base0)(step0)

    val diagInd = induc(m :-> AreEqual(m)(m))

    val p = AreEqual(m)(m).sym

    val diag = diagInd(Star)(m :~> (p :-> p))

    // assert(diag.typ == n ~>: (AreEqual(n)(n)), "wrong induction family")

    val target = n :~> (m :~> (("_" :: (n =:= m)) :-> AreEqual(n)(m)))

    val indEq = IdentityTyp.induc(NatTyp, target)

    val decEqThm = n ~>: (m ~>: ((n =:= m) ->: AreEqual(n)(m)))

    val decEqPf = indEq(diag) 

    // assert(decEqPf.typ == decEqThm, "Proved the wrong theorem")


}