package indigojs.delegates

final class SceneAudioSourceDelegate(val bindingKey: String, val playbackPattern: PlaybackPatternDelegate, val masterVolume: VolumeDelegate)

object SceneAudioSourceDelegate {

  val None: SceneAudioSourceDelegate =
    new SceneAudioSourceDelegate("none", PlaybackPatternDelegate.Silent, VolumeDelegate.Min)

}