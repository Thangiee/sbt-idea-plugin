package org.jetbrains.sbtidea.download

import java.io.{FileNotFoundException, InputStream}

import org.jetbrains.sbtidea.Keys
import org.jetbrains.sbtidea.download.api.IdeaResolver
import sbt.{URL, url}

trait JBIdeaRepoArtifactResolver extends IdeaResolver {

  override def resolveUrlForIdeaBuild(idea: BuildInfo): Seq[ArtifactPart] = {
    val (build, edition)  = (idea.buildNumber, idea.edition.name)
    val ideaUrl           = getUrl(idea, ".zip")
    // sources are available only for Community Edition
    val srcJarUrl         = getUrl(idea.copy(edition = Keys.IntelliJPlatform.IdeaCommunity), "-sources.jar")

    ArtifactPart(ideaUrl, ArtifactKind.IDEA_DIST, s"$edition-$build.zip") ::
      ArtifactPart(srcJarUrl, ArtifactKind.IDEA_SRC, s"$edition-$build-sources.jar", optional = true) :: Nil
  }

  //noinspection NoTailRecursionAnnotation
  protected def getUrl(platform: BuildInfo, artifactSuffix: String, trySnapshot: Boolean = false): URL = {
    val (repo, suffix)  =
      if      (trySnapshot)                               "snapshots" -> "-EAP-SNAPSHOT"
      else if (platform.buildNumber.contains("SNAPSHOT")) "snapshots" -> ""
      else                                                "releases"  -> ""
    val baseUrl         = s"https://www.jetbrains.com/intellij-repository/$repo/com/jetbrains/intellij/${platform.edition.platformPrefix}"
    val build           = platform.buildNumber + suffix
    var stream: Option[InputStream] = None
    try {
      val result  = url(s"$baseUrl/${platform.edition.name}/$build/${platform.edition.name}-$build$artifactSuffix")
      stream      = Some(result.openStream())
      result
    } catch {
      case _: FileNotFoundException if !trySnapshot && !platform.buildNumber.endsWith("SNAPSHOT") =>
        println(s"Can't find $platform in releases, trying snapshots")
        getUrl(platform, artifactSuffix, trySnapshot = true)
    } finally {
      stream.foreach(_.close())
    }
  }

}
