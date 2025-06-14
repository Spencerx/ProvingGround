package provingground.library
import provingground._
import HoTT._

import interface._
import monix.eval.Task
object LeanMemo {
  def defMap: Map[trepplein.Name, Term] =
    Map(
      trepplein.Name("nat", "brec_on") -> nat$brec_on.value,
      trepplein.Name("has_zero")       -> ("has_zero" :: FuncTyp(Type, Type)),
      trepplein.Name("nat", "mul")     -> nat$mul.value,
      trepplein.Name("nat", "has_add") -> nat$has_add.value,
      trepplein.Name("has_one")        -> ("has_one" :: FuncTyp(Type, Type)),
      trepplein.Name("punit", "star")  -> ("punit.star" :: "punit" :: Type),
      trepplein.Name("nat", "zero")    -> ("nat.zero" :: "nat" :: Type),
      trepplein.Name("nat", "pow")     -> nat$pow.value,
      trepplein.Name("nat", "succ") -> ("nat.succ" :: FuncTyp(
        "nat" :: Type,
        "nat" :: Type
      )),
      trepplein
        .Name("nat", "less_than_or_equal", "refl") -> ("nat.less_than_or_equal.refl" :: piDefn(
        "'c_308702732" :: "nat" :: Type
      )(
        ("nat.less_than_or_equal" :: FuncTyp(
          "nat" :: Type,
          FuncTyp("nat" :: Type, Prop)
        ))("'c_308702732" :: "nat" :: Type)("'c_308702732" :: "nat" :: Type)
      )),
      trepplein.Name("has_add", "add") -> has_add$add.value,
      trepplein
        .Name("pprod", "mk") -> ("pprod.mk" :: piDefn("'f_1394520732" :: Type)(
        piDefn("'g_1655163109" :: Type)(
          FuncTyp(
            "'f_1394520732" :: Type,
            FuncTyp(
              "'g_1655163109" :: Type,
              ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(
                "'f_1394520732" :: Type
              )("'g_1655163109" :: Type)
            )
          )
        )
      )),
      trepplein
        .Name("pprod")                   -> ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type))),
      trepplein.Name("has_zero", "zero") -> has_zero$zero.value,
      trepplein.Name("has_zero", "mk") -> ("has_zero.mk" :: piDefn(
        "'c_1531913461" :: Type
      )(
        FuncTyp(
          "'c_1531913461" :: Type,
          ("has_zero" :: FuncTyp(Type, Type))("'c_1531913461" :: Type)
        )
      )),
      trepplein.Name("has_mul", "mk") -> ("has_mul.mk" :: piDefn(
        "'d_271481947" :: Type
      )(
        FuncTyp(
          FuncTyp(
            "'d_271481947" :: Type,
            FuncTyp("'d_271481947" :: Type, "'d_271481947" :: Type)
          ),
          ("has_mul" :: FuncTyp(Type, Type))("'d_271481947" :: Type)
        )
      )),
      trepplein.Name("punit")               -> ("punit" :: Type),
      trepplein.Name("nat", "add", "_main") -> nat$add$_main.value,
      trepplein.Name("has_add")             -> ("has_add" :: FuncTyp(Type, Type)),
      trepplein.Name("nat", "succ_le_succ") -> nat$succ_le_succ.value,
      trepplein.Name("has_one", "one")      -> has_one$one.value,
      trepplein.Name("nat", "below")        -> nat$below.value,
      trepplein
        .Name("nat", "less_than_or_equal") -> ("nat.less_than_or_equal" :: FuncTyp(
        "nat" :: Type,
        FuncTyp("nat" :: Type, Prop)
      )),
      trepplein.Name("id_rhs")  -> id_rhs.value,
      trepplein.Name("has_mul") -> ("has_mul" :: FuncTyp(Type, Type)),
      trepplein
        .Name("nat", "less_than_or_equal", "step") -> ("nat.less_than_or_equal.step" :: piDefn(
        "'e_633342745" :: "nat" :: Type
      )(
        piDefn("'f_132650318" :: "nat" :: Type)(
          FuncTyp(
            ("nat.less_than_or_equal" :: FuncTyp(
              "nat" :: Type,
              FuncTyp("nat" :: Type, Prop)
            ))("'e_633342745" :: "nat" :: Type)(
              "'f_132650318" :: "nat" :: Type
            ),
            ("nat.less_than_or_equal" :: FuncTyp(
              "nat" :: Type,
              FuncTyp("nat" :: Type, Prop)
            ))("'e_633342745" :: "nat" :: Type)(
              ("nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type))(
                "'f_132650318" :: "nat" :: Type
              )
            )
          )
        )
      )),
      trepplein.Name("nat", "has_le")   -> nat$has_le.value,
      trepplein.Name("has_mul", "mul")  -> has_mul$mul.value,
      trepplein.Name("nat", "cases_on") -> nat$cases_on.value,
      trepplein.Name("nat", "le_refl")  -> nat$le_refl.value,
      trepplein.Name("has_add", "mk") -> ("has_add.mk" :: piDefn(
        "'d_1188183567" :: Type
      )(
        FuncTyp(
          FuncTyp(
            "'d_1188183567" :: Type,
            FuncTyp("'d_1188183567" :: Type, "'d_1188183567" :: Type)
          ),
          ("has_add" :: FuncTyp(Type, Type))("'d_1188183567" :: Type)
        )
      )),
      trepplein.Name("nat")                 -> ("nat" :: Type),
      trepplein.Name("has_le", "le")        -> has_le$le.value,
      trepplein.Name("nat", "has_one")      -> nat$has_one.value,
      trepplein.Name("nat", "pow", "_main") -> nat$pow$_main.value,
      trepplein.Name("pprod", "fst")        -> pprod$fst.value,
      trepplein.Name("has_le")              -> ("has_le" :: FuncTyp(Type, Type)),
      trepplein.Name("has_le", "mk") -> ("has_le.mk" :: piDefn(
        "'d_1129063827" :: Type
      )(
        FuncTyp(
          FuncTyp(
            "'d_1129063827" :: Type,
            FuncTyp("'d_1129063827" :: Type, Prop)
          ),
          ("has_le" :: FuncTyp(Type, Type))("'d_1129063827" :: Type)
        )
      )),
      trepplein.Name("nat", "add")     -> nat$add.value,
      trepplein.Name("nat", "has_mul") -> nat$has_mul.value,
      trepplein.Name("has_one", "mk") -> ("has_one.mk" :: piDefn(
        "'c_264172685" :: Type
      )(
        FuncTyp(
          "'c_264172685" :: Type,
          ("has_one" :: FuncTyp(Type, Type))("'c_264172685" :: Type)
        )
      )),
      trepplein.Name("nat", "has_zero")     -> nat$has_zero.value,
      trepplein.Name("nat", "mul", "_main") -> nat$mul$_main.value
    )
  def indMap: Map[trepplein.Name, TermIndMod] =
    Map(
      trepplein.Name("has_zero") -> SimpleIndMod(
        trepplein.Name("has_zero"),
        "has_zero" :: FuncTyp(Type, Type),
        Vector(
          "has_zero.mk" :: piDefn("'c_1531913461" :: Type)(
            FuncTyp(
              "'c_1531913461" :: Type,
              ("has_zero" :: FuncTyp(Type, Type))("'c_1531913461" :: Type)
            )
          )
        ),
        1,
        false
      ),
      trepplein.Name("has_one") -> SimpleIndMod(
        trepplein.Name("has_one"),
        "has_one" :: FuncTyp(Type, Type),
        Vector(
          "has_one.mk" :: piDefn("'c_264172685" :: Type)(
            FuncTyp(
              "'c_264172685" :: Type,
              ("has_one" :: FuncTyp(Type, Type))("'c_264172685" :: Type)
            )
          )
        ),
        1,
        false
      ),
      trepplein.Name("pprod") -> SimpleIndMod(
        trepplein.Name("pprod"),
        "pprod" :: FuncTyp(Type, FuncTyp(Type, Type)),
        Vector(
          "pprod.mk" :: piDefn("'f_1394520732" :: Type)(
            piDefn("'g_1655163109" :: Type)(
              FuncTyp(
                "'f_1394520732" :: Type,
                FuncTyp(
                  "'g_1655163109" :: Type,
                  ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(
                    "'f_1394520732" :: Type
                  )("'g_1655163109" :: Type)
                )
              )
            )
          )
        ),
        2,
        false
      ),
      trepplein.Name("punit") -> SimpleIndMod(
        trepplein.Name("punit"),
        "punit" :: Type,
        Vector("punit.star" :: "punit" :: Type),
        0,
        false
      ),
      trepplein.Name("has_add") -> SimpleIndMod(
        trepplein.Name("has_add"),
        "has_add" :: FuncTyp(Type, Type),
        Vector(
          "has_add.mk" :: piDefn("'d_1188183567" :: Type)(
            FuncTyp(
              FuncTyp(
                "'d_1188183567" :: Type,
                FuncTyp("'d_1188183567" :: Type, "'d_1188183567" :: Type)
              ),
              ("has_add" :: FuncTyp(Type, Type))("'d_1188183567" :: Type)
            )
          )
        ),
        1,
        false
      ),
      trepplein.Name("nat", "less_than_or_equal") -> IndexedIndMod(
        trepplein.Name("nat", "less_than_or_equal"),
        "nat.less_than_or_equal" :: FuncTyp(
          "nat" :: Type,
          FuncTyp("nat" :: Type, Prop)
        ),
        Vector(
          "nat.less_than_or_equal.refl" :: piDefn(
            "'c_308702732" :: "nat" :: Type
          )(
            ("nat.less_than_or_equal" :: FuncTyp(
              "nat" :: Type,
              FuncTyp("nat" :: Type, Prop)
            ))("'c_308702732" :: "nat" :: Type)("'c_308702732" :: "nat" :: Type)
          ),
          "nat.less_than_or_equal.step" :: piDefn(
            "'e_633342745" :: "nat" :: Type
          )(
            piDefn("'f_132650318" :: "nat" :: Type)(
              FuncTyp(
                ("nat.less_than_or_equal" :: FuncTyp(
                  "nat" :: Type,
                  FuncTyp("nat" :: Type, Prop)
                ))("'e_633342745" :: "nat" :: Type)(
                  "'f_132650318" :: "nat" :: Type
                ),
                ("nat.less_than_or_equal" :: FuncTyp(
                  "nat" :: Type,
                  FuncTyp("nat" :: Type, Prop)
                ))("'e_633342745" :: "nat" :: Type)(
                  ("nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type))(
                    "'f_132650318" :: "nat" :: Type
                  )
                )
              )
            )
          )
        ),
        1,
        true
      ),
      trepplein.Name("has_mul") -> SimpleIndMod(
        trepplein.Name("has_mul"),
        "has_mul" :: FuncTyp(Type, Type),
        Vector(
          "has_mul.mk" :: piDefn("'d_271481947" :: Type)(
            FuncTyp(
              FuncTyp(
                "'d_271481947" :: Type,
                FuncTyp("'d_271481947" :: Type, "'d_271481947" :: Type)
              ),
              ("has_mul" :: FuncTyp(Type, Type))("'d_271481947" :: Type)
            )
          )
        ),
        1,
        false
      ),
      trepplein.Name("nat") -> SimpleIndMod(
        trepplein.Name("nat"),
        "nat" :: Type,
        Vector(
          "nat.zero" :: "nat" :: Type,
          "nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type)
        ),
        0,
        false
      ),
      trepplein.Name("has_le") -> SimpleIndMod(
        trepplein.Name("has_le"),
        "has_le" :: FuncTyp(Type, Type),
        Vector(
          "has_le.mk" :: piDefn("'d_1129063827" :: Type)(
            FuncTyp(
              FuncTyp(
                "'d_1129063827" :: Type,
                FuncTyp("'d_1129063827" :: Type, Prop)
              ),
              ("has_le" :: FuncTyp(Type, Type))("'d_1129063827" :: Type)
            )
          )
        ),
        1,
        false
      )
    )
  def defTaskMap: Map[trepplein.Name, Task[Term]] =
    Map(
      trepplein.Name("nat", "brec_on") -> Task(nat$brec_on.value),
      trepplein.Name("has_zero")       -> Task("has_zero" :: FuncTyp(Type, Type)),
      trepplein.Name("nat", "mul")     -> Task(nat$mul.value),
      trepplein.Name("nat", "has_add") -> Task(nat$has_add.value),
      trepplein.Name("has_one")        -> Task("has_one" :: FuncTyp(Type, Type)),
      trepplein.Name("punit", "star")  -> Task("punit.star" :: "punit" :: Type),
      trepplein.Name("nat", "zero")    -> Task("nat.zero" :: "nat" :: Type),
      trepplein.Name("nat", "pow")     -> Task(nat$pow.value),
      trepplein.Name("nat", "succ") -> Task(
        "nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type)
      ),
      trepplein.Name("nat", "less_than_or_equal", "refl") -> Task(
        "nat.less_than_or_equal.refl" :: piDefn(
          "'c_308702732" :: "nat" :: Type
        )(
          ("nat.less_than_or_equal" :: FuncTyp(
            "nat" :: Type,
            FuncTyp("nat" :: Type, Prop)
          ))("'c_308702732" :: "nat" :: Type)("'c_308702732" :: "nat" :: Type)
        )
      ),
      trepplein.Name("has_add", "add") -> Task(has_add$add.value),
      trepplein.Name("pprod", "mk") -> Task(
        "pprod.mk" :: piDefn("'f_1394520732" :: Type)(
          piDefn("'g_1655163109" :: Type)(
            FuncTyp(
              "'f_1394520732" :: Type,
              FuncTyp(
                "'g_1655163109" :: Type,
                ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(
                  "'f_1394520732" :: Type
                )("'g_1655163109" :: Type)
              )
            )
          )
        )
      ),
      trepplein.Name("pprod") -> Task(
        "pprod" :: FuncTyp(Type, FuncTyp(Type, Type))
      ),
      trepplein.Name("has_zero", "zero") -> Task(has_zero$zero.value),
      trepplein.Name("has_zero", "mk") -> Task(
        "has_zero.mk" :: piDefn("'c_1531913461" :: Type)(
          FuncTyp(
            "'c_1531913461" :: Type,
            ("has_zero" :: FuncTyp(Type, Type))("'c_1531913461" :: Type)
          )
        )
      ),
      trepplein.Name("has_mul", "mk") -> Task(
        "has_mul.mk" :: piDefn("'d_271481947" :: Type)(
          FuncTyp(
            FuncTyp(
              "'d_271481947" :: Type,
              FuncTyp("'d_271481947" :: Type, "'d_271481947" :: Type)
            ),
            ("has_mul" :: FuncTyp(Type, Type))("'d_271481947" :: Type)
          )
        )
      ),
      trepplein.Name("punit")               -> Task("punit" :: Type),
      trepplein.Name("nat", "add", "_main") -> Task(nat$add$_main.value),
      trepplein.Name("has_add")             -> Task("has_add" :: FuncTyp(Type, Type)),
      trepplein.Name("nat", "succ_le_succ") -> Task(nat$succ_le_succ.value),
      trepplein.Name("has_one", "one")      -> Task(has_one$one.value),
      trepplein.Name("nat", "below")        -> Task(nat$below.value),
      trepplein.Name("nat", "less_than_or_equal") -> Task(
        "nat.less_than_or_equal" :: FuncTyp(
          "nat" :: Type,
          FuncTyp("nat" :: Type, Prop)
        )
      ),
      trepplein.Name("id_rhs")  -> Task(id_rhs.value),
      trepplein.Name("has_mul") -> Task("has_mul" :: FuncTyp(Type, Type)),
      trepplein.Name("nat", "less_than_or_equal", "step") -> Task(
        "nat.less_than_or_equal.step" :: piDefn(
          "'e_633342745" :: "nat" :: Type
        )(
          piDefn("'f_132650318" :: "nat" :: Type)(
            FuncTyp(
              ("nat.less_than_or_equal" :: FuncTyp(
                "nat" :: Type,
                FuncTyp("nat" :: Type, Prop)
              ))("'e_633342745" :: "nat" :: Type)(
                "'f_132650318" :: "nat" :: Type
              ),
              ("nat.less_than_or_equal" :: FuncTyp(
                "nat" :: Type,
                FuncTyp("nat" :: Type, Prop)
              ))("'e_633342745" :: "nat" :: Type)(
                ("nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type))(
                  "'f_132650318" :: "nat" :: Type
                )
              )
            )
          )
        )
      ),
      trepplein.Name("nat", "has_le")   -> Task(nat$has_le.value),
      trepplein.Name("has_mul", "mul")  -> Task(has_mul$mul.value),
      trepplein.Name("nat", "cases_on") -> Task(nat$cases_on.value),
      trepplein.Name("nat", "le_refl")  -> Task(nat$le_refl.value),
      trepplein.Name("has_add", "mk") -> Task(
        "has_add.mk" :: piDefn("'d_1188183567" :: Type)(
          FuncTyp(
            FuncTyp(
              "'d_1188183567" :: Type,
              FuncTyp("'d_1188183567" :: Type, "'d_1188183567" :: Type)
            ),
            ("has_add" :: FuncTyp(Type, Type))("'d_1188183567" :: Type)
          )
        )
      ),
      trepplein.Name("nat")                 -> Task("nat" :: Type),
      trepplein.Name("has_le", "le")        -> Task(has_le$le.value),
      trepplein.Name("nat", "has_one")      -> Task(nat$has_one.value),
      trepplein.Name("nat", "pow", "_main") -> Task(nat$pow$_main.value),
      trepplein.Name("pprod", "fst")        -> Task(pprod$fst.value),
      trepplein.Name("has_le")              -> Task("has_le" :: FuncTyp(Type, Type)),
      trepplein.Name("has_le", "mk") -> Task(
        "has_le.mk" :: piDefn("'d_1129063827" :: Type)(
          FuncTyp(
            FuncTyp(
              "'d_1129063827" :: Type,
              FuncTyp("'d_1129063827" :: Type, Prop)
            ),
            ("has_le" :: FuncTyp(Type, Type))("'d_1129063827" :: Type)
          )
        )
      ),
      trepplein.Name("nat", "add")     -> Task(nat$add.value),
      trepplein.Name("nat", "has_mul") -> Task(nat$has_mul.value),
      trepplein.Name("has_one", "mk") -> Task(
        "has_one.mk" :: piDefn("'c_264172685" :: Type)(
          FuncTyp(
            "'c_264172685" :: Type,
            ("has_one" :: FuncTyp(Type, Type))("'c_264172685" :: Type)
          )
        )
      ),
      trepplein.Name("nat", "has_zero")     -> Task(nat$has_zero.value),
      trepplein.Name("nat", "mul", "_main") -> Task(nat$mul$_main.value)
    )
  def indTaskMap: Map[trepplein.Name, Task[TermIndMod]] =
    Map(
      trepplein.Name("has_zero") -> Task(
        SimpleIndMod(
          trepplein.Name("has_zero"),
          "has_zero" :: FuncTyp(Type, Type),
          Vector(
            "has_zero.mk" :: piDefn("'c_1531913461" :: Type)(
              FuncTyp(
                "'c_1531913461" :: Type,
                ("has_zero" :: FuncTyp(Type, Type))("'c_1531913461" :: Type)
              )
            )
          ),
          1,
          false
        )
      ),
      trepplein.Name("has_one") -> Task(
        SimpleIndMod(
          trepplein.Name("has_one"),
          "has_one" :: FuncTyp(Type, Type),
          Vector(
            "has_one.mk" :: piDefn("'c_264172685" :: Type)(
              FuncTyp(
                "'c_264172685" :: Type,
                ("has_one" :: FuncTyp(Type, Type))("'c_264172685" :: Type)
              )
            )
          ),
          1,
          false
        )
      ),
      trepplein.Name("pprod") -> Task(
        SimpleIndMod(
          trepplein.Name("pprod"),
          "pprod" :: FuncTyp(Type, FuncTyp(Type, Type)),
          Vector(
            "pprod.mk" :: piDefn("'f_1394520732" :: Type)(
              piDefn("'g_1655163109" :: Type)(
                FuncTyp(
                  "'f_1394520732" :: Type,
                  FuncTyp(
                    "'g_1655163109" :: Type,
                    ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(
                      "'f_1394520732" :: Type
                    )("'g_1655163109" :: Type)
                  )
                )
              )
            )
          ),
          2,
          false
        )
      ),
      trepplein.Name("punit") -> Task(
        SimpleIndMod(
          trepplein.Name("punit"),
          "punit" :: Type,
          Vector("punit.star" :: "punit" :: Type),
          0,
          false
        )
      ),
      trepplein.Name("has_add") -> Task(
        SimpleIndMod(
          trepplein.Name("has_add"),
          "has_add" :: FuncTyp(Type, Type),
          Vector(
            "has_add.mk" :: piDefn("'d_1188183567" :: Type)(
              FuncTyp(
                FuncTyp(
                  "'d_1188183567" :: Type,
                  FuncTyp("'d_1188183567" :: Type, "'d_1188183567" :: Type)
                ),
                ("has_add" :: FuncTyp(Type, Type))("'d_1188183567" :: Type)
              )
            )
          ),
          1,
          false
        )
      ),
      trepplein.Name("nat", "less_than_or_equal") -> Task(
        IndexedIndMod(
          trepplein.Name("nat", "less_than_or_equal"),
          "nat.less_than_or_equal" :: FuncTyp(
            "nat" :: Type,
            FuncTyp("nat" :: Type, Prop)
          ),
          Vector(
            "nat.less_than_or_equal.refl" :: piDefn(
              "'c_308702732" :: "nat" :: Type
            )(
              ("nat.less_than_or_equal" :: FuncTyp(
                "nat" :: Type,
                FuncTyp("nat" :: Type, Prop)
              ))("'c_308702732" :: "nat" :: Type)(
                "'c_308702732" :: "nat" :: Type
              )
            ),
            "nat.less_than_or_equal.step" :: piDefn(
              "'e_633342745" :: "nat" :: Type
            )(
              piDefn("'f_132650318" :: "nat" :: Type)(
                FuncTyp(
                  ("nat.less_than_or_equal" :: FuncTyp(
                    "nat" :: Type,
                    FuncTyp("nat" :: Type, Prop)
                  ))("'e_633342745" :: "nat" :: Type)(
                    "'f_132650318" :: "nat" :: Type
                  ),
                  ("nat.less_than_or_equal" :: FuncTyp(
                    "nat" :: Type,
                    FuncTyp("nat" :: Type, Prop)
                  ))("'e_633342745" :: "nat" :: Type)(
                    ("nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type))(
                      "'f_132650318" :: "nat" :: Type
                    )
                  )
                )
              )
            )
          ),
          1,
          true
        )
      ),
      trepplein.Name("has_mul") -> Task(
        SimpleIndMod(
          trepplein.Name("has_mul"),
          "has_mul" :: FuncTyp(Type, Type),
          Vector(
            "has_mul.mk" :: piDefn("'d_271481947" :: Type)(
              FuncTyp(
                FuncTyp(
                  "'d_271481947" :: Type,
                  FuncTyp("'d_271481947" :: Type, "'d_271481947" :: Type)
                ),
                ("has_mul" :: FuncTyp(Type, Type))("'d_271481947" :: Type)
              )
            )
          ),
          1,
          false
        )
      ),
      trepplein.Name("nat") -> Task(
        SimpleIndMod(
          trepplein.Name("nat"),
          "nat" :: Type,
          Vector(
            "nat.zero" :: "nat" :: Type,
            "nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type)
          ),
          0,
          false
        )
      ),
      trepplein.Name("has_le") -> Task(
        SimpleIndMod(
          trepplein.Name("has_le"),
          "has_le" :: FuncTyp(Type, Type),
          Vector(
            "has_le.mk" :: piDefn("'d_1129063827" :: Type)(
              FuncTyp(
                FuncTyp(
                  "'d_1129063827" :: Type,
                  FuncTyp("'d_1129063827" :: Type, Prop)
                ),
                ("has_le" :: FuncTyp(Type, Type))("'d_1129063827" :: Type)
              )
            )
          ),
          1,
          false
        )
      )
    )
}
