package provingground.translation

import provingground._, HoTT._
import scala.language.existentials

object RefineTerms {
  def refine(term: Term): Term = term match {
    case LambdaFixed(variable: Term, value: Term) =>
      val newvar = refine(variable)
      val newval = refine(value)
      val vartyp = newvar.typ
      val valtyp = newval.typ
      LambdaFixed[vartyp.Obj, valtyp.Obj](newvar.asInstanceOf[vartyp.Obj],
                                          newval.asInstanceOf[valtyp.Obj])
    case LambdaTerm(variable: Term, value: Term) =>
      val newvar = refine(variable)
      val newval = refine(value)
      val vartyp = newvar.typ
      val valtyp = newval.typ
      LambdaTerm[vartyp.Obj, valtyp.Obj](newvar.asInstanceOf[vartyp.Obj],
                                         newval.asInstanceOf[valtyp.Obj])
    case sym: Symbolic =>
      refineTyp(sym.typ).symbObj(sym.name)
    case _ => term
  }

  def refineTyp(typ: Typ[Term]): Typ[Term] = typ match {
    case FuncTyp(dom: Typ[u], codom: Typ[v]) =>
      val newdom = refineTyp(dom)
      val newcod = refineTyp(codom)
      FuncTyp[newdom.Obj, newcod.Obj](newdom.asInstanceOf[Typ[newdom.Obj]],
                                      newcod.asInstanceOf[Typ[newcod.Obj]])
    case PiDefn(variable: Term, value: Typ[v]) =>
      PiDefn(refine(variable), refineTyp(value))
    case PiTyp(fibre) =>
      refine(fibre) match {
        case newFibre: Func[u, _] =>
          newFibre.codom.obj match {
            case _: Typ[v] =>
              PiDefn(newFibre.asInstanceOf[Func[u, Typ[v]]])
          }
        case _ => typ
      }
    case _ => typ
  }
}
