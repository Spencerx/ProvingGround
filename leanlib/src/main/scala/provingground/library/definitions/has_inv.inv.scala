package provingground.library
import provingground._
import HoTT._
import induction._
object has_inv$inv {
  lazy val value = lambda("'i_2072061174" :: Type)(
    ({
      val rxyz = has_invInd
        .value("'i_2072061174" :: Type)
        .rec(FuncTyp("'i_2072061174" :: Type, "'i_2072061174" :: Type))
      rxyz
    })(
      lmbda("'k_806845746" :: FuncTyp("'i_2072061174" :: Type,
                                      "'i_2072061174" :: Type))(
        "'k_806845746" :: FuncTyp("'i_2072061174" :: Type,
                                  "'i_2072061174" :: Type))))
}
