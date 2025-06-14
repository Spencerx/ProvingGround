package provingground.learning
import scala.concurrent._
import scala.reflect.runtime.universe._
import shapeless._
import provingground.learning.TypedPostResponse.MicroBot

/**
  * Typeclass for being able to post with content type P in W
  */
trait Postable[P, W, ID] {
  def post(content: P, web: W, pred: Set[ID]): Future[ID]
  implicit val tag: TypeTag[P]
}

trait BiPostable[P, W, ID] extends Postable[P, W, ID] {
  def postAt(content: P, web: W, id: ID, pred: Set[ID]): Future[Unit]

  def allPosts(web: W): Vector[(P, ID, Set[ID])]

  def postAll(web: W, data: Seq[(P, ID, Set[ID])]) = data.foreach {
    case (p, id, pred) => postAt(p, web, id, pred)
  }
}

object Postable {
  implicit val ec: scala.concurrent.ExecutionContext =
    provingground.Utils.ec

  /**
    * Making a postable
    *
    * @param postFunc the posting function
    * @return a Postable
    */
  def apply[P: TypeTag, W, ID](
      postFunc: (P, W, Set[ID]) => Future[ID]
  ): Postable[P, W, ID] =
    Impl(postFunc)

  /**
    * A concrete implementation of postable
    *
    * @param postFunc the posting function
    */
  case class Impl[P, W, ID](postFunc: (P, W, Set[ID]) => Future[ID])(
      implicit val tag: TypeTag[P]
  ) extends Postable[P, W, ID] {
    def post(content: P, web: W, pred: Set[ID]): Future[ID] =
      postFunc(content, web, pred)
  }

  /**
    * post and return post data
    *
    * @param content content to be posted
    * @param web web where we post
    * @param pred predecessors of the post
    * @param pw postability of the type
    * @return PostData as a future
    */
  def post[P, W, ID](content: P, web: W, pred: Set[ID])(
      implicit pw: Postable[P, W, ID], dg: DataGetter[P, W, ID]
  ): Future[PostData[_, W, ID]] =
    pw.post(content, web, pred).map { id =>
      PostData.get(content, id)
    } // FIXME transform the data

  /**
    * post in a buffer
    *
    * @return postability
    */
  implicit def bufferPostable[P: TypeTag, ID, W <: PostBuffer[P, ID]]
      : Postable[P, W, ID] = {
    def post(content: P, web: W, pred: Set[ID]): Future[ID] =
      web.post(content, pred)
    Impl(post)
  }

  /**
    * post an Option[P], posting a Unit in case of `None`
    *
    * @param pw postability of `P`
    * @param uw postability of a Unit
    * @return postability of `Option[P]`
    */
  implicit def optionPostable[P: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      uw: Postable[Unit, W, ID]
  ): Postable[Option[P], W, ID] = {
    def post(content: Option[P], web: W, pred: Set[ID]): Future[ID] =
      content.fold(uw.post((), web, pred))(c => pw.post(c, web, pred))
    Impl(post)
  }

  implicit def setPostable[P: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      uw: Postable[Unit, W, ID]
  ): Postable[Set[P], W, ID] =
    new Postable[Set[P], W, ID] {
      def post(content: Set[P], web: W, pred: Set[ID]): Future[ID] =
        content.headOption
          .map { x =>
            val tail = content - x
            pw.post(x, web, pred)
              .flatMap(
                pid =>
                  if (tail.isEmpty) Future(pid) else post(tail, web, Set(pid))
              )
          }
          .getOrElse(uw.post((), web, pred))

      val tag: reflect.runtime.universe.TypeTag[Set[P]] = implicitly

    }

  /**
    * post a `Try[P]` by posting `P` or an error
    *
    * @param pw postability of `P`
    * @param ew postability of a `Throwable`
    * @return postability of `Try[P]`
    */
  implicit def tryPostable[P: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      ew: Postable[Throwable, W, ID]
  ): Postable[util.Try[P], W, ID] = {
    def post(content: util.Try[P], web: W, pred: Set[ID]): Future[ID] =
      content.fold(err => ew.post(err, web, pred), c => pw.post(c, web, pred))

    Impl(post)
  }

  implicit def eitherPostable[P: TypeTag, Q: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      qw: Postable[Q, W, ID]
  ): Postable[Either[P, Q], W, ID] = {
    def post(content: Either[P, Q], web: W, pred: Set[ID]): Future[ID] =
      content.fold(fa => pw.post(fa, web, pred), fb => qw.post(fb, web, pred))
    Impl(post)
  }

  implicit def pairPost[P: TypeTag, Q <: HList: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      qw: Postable[Q, W, ID]
  ): Postable[P :: Q, W, ID] = {
    def post(content: P :: Q, web: W, pred: Set[ID]): Future[ID] =
      content match {
        case p :: HNil =>
          pw.post(p, web, pred)
        case p :: q =>
          pw.post(p, web, pred).flatMap(id => qw.post(q, web, Set(id)))
      }
    Impl(post)
  }

  /**
    * Post a `Right[P]`, to be used to split the stream with a change or not.
    *
    * @param pw postability of `P`
    * @param uw postability of `Unit`
    * @return Postability of `Right[P]`
    */
  implicit def rightPost[X: TypeTag, P: TypeTag, W, ID](
      implicit pw: Postable[P, W, ID],
      uw: Postable[Unit, W, ID]
  ): Postable[Right[X, P], W, ID] = {
    def post(content: Right[X, P], web: W, pred: Set[ID]): Future[ID] = {
      uw.post((), web, pred).foreach(_ => ())
      pw.post(content.value, web, pred)
    }

    Impl(post)
  }

}

/**
  * Data for a post, including the implicit saying it is postable
  * @param content the content of the post
  * @param id the index of the post, returned after posting
  */
case class PostData[P, W, ID](content: P, id: ID)(
    implicit val pw: Postable[P, W, ID]
) {

  def getOpt[Q](implicit qw: Postable[Q, W, ID]): Option[Q] =
    if (pw.tag.tpe =:= qw.tag.tpe) Some(content.asInstanceOf[Q]) else None

  def getTagOpt[Q](implicit tag: TypeTag[Q]): Option[Q] =
    if (pw.tag.tpe =:= tag.tpe) Some(content.asInstanceOf[Q]) else None
}

object PostData {
  def get[P, W, ID](content: P, id: ID)(implicit dg: DataGetter[P, W, ID]) =
    dg.data(content, id)
}

trait DataGetter[P, W, ID] {
  def data(content: P, id: ID): PostData[_, W, ID]
}

object DataGetter extends SimpleDataGetter{
  implicit def pairData[P: TypeTag, Q <: HList: TypeTag, W, ID](
      implicit pd: DataGetter[P, W, ID],
      qd: DataGetter[Q, W, ID]
  ): DataGetter[P :: Q, W, ID] = new DataGetter[P :: Q, W, ID] {
    def data(content: P :: Q, id: ID): PostData[_, W, ID] = 
      content match {
        case p :: HNil =>
          pd.data(p, id)
        case p :: q =>
          qd.data(q, id)
      }    
  }

  implicit def optionData[P: TypeTag, W, ID](
      implicit pd: DataGetter[P, W, ID],
      uw: Postable[Unit, W, ID]
  ) : DataGetter[Option[P], W, ID] = new DataGetter[Option[P], W, ID] {
    def data(content: Option[P], id: ID): PostData[_, W, ID] = 
      content match {
        case None => PostData((), id)
        case Some(value) => 
          pd.data(value, id) 
      }
  }

  implicit def setData[P: TypeTag, W, ID](
      implicit pd: DataGetter[P, W, ID],
      uw: Postable[Unit, W, ID]
  ) : DataGetter[Set[P], W, ID] = new DataGetter[Set[P], W, ID]{
    def data(content: Set[P], id: ID): PostData[_, W, ID] = 
      content.lastOption.map{
        x => pd.data(x, id)
      }.getOrElse(PostData((), id))
  } 

  implicit def eitherData[P, Q, W, ID](
    implicit pd: DataGetter[P, W, ID],
    qd: DataGetter[Q, W, ID]
  ) : DataGetter[Either[P, Q], W, ID] = new DataGetter[Either[P, Q], W, ID] {
    def data(content: Either[P,Q], id: ID): PostData[_, W, ID] = content match {
      case Left(value) => pd.data(value, id)
      case Right(value) => qd.data(value, id)
    }
  }
}

trait SimpleDataGetter {
  implicit def getter[P, W, ID](implicit pw: Postable[P, W, ID]) : DataGetter[P, W, ID] =
    new DataGetter[P, W, ID] {
      def data(content: P, id: ID): PostData[_, W, ID] = PostData(content, id)
    }
}
