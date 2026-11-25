package com.christmasdream.launcher
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
/** Quiet synthesized public-domain Jingle Bells motif. */
class JingleMusicPlayer {
 private val handler=Handler(Looper.getMainLooper()); private var enabled=false; private var generation=0; private val active=mutableSetOf<AudioTrack>()
 private val notes=intArrayOf(988,1318,1568,1318,659,659,659,659,659,659,659,784,523,587,659,698,698,698,698,698,659,659,659,659,587,587,659,587,784)
 private val durations=intArrayOf(220,220,420,320,260,260,520,260,260,520,260,260,390,130,780,260,260,390,130,260,260,260,130,130,260,260,260,520,520)
 fun start(){if(enabled)return;enabled=true;val token=++generation;play(0,token)}
 fun stop(){enabled=false;generation++;handler.removeCallbacksAndMessages(null);active.toList().forEach{try{it.pause();it.flush();it.release()}catch(_:Exception){}};active.clear()}
 private fun play(index:Int,token:Int){if(!enabled||token!=generation)return;val i=index%notes.size
  try{val rate=22050;val ms=durations[i];val count=rate*ms/1000
   val data=ShortArray(count){n->val t=n.toDouble()/rate;val env=exp(-2.8*t/(ms/1000.0));((sin(2*PI*notes[i]*t)+0.34*sin(2*PI*notes[i]*2*t)+0.12*sin(2*PI*notes[i]*3*t))/1.46*env*0.055*Short.MAX_VALUE).toInt().toShort()}
   val track=AudioTrack(AudioManager.STREAM_MUSIC,rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,data.size*2,AudioTrack.MODE_STATIC)
   track.setVolume(0.12f);track.write(data,0,data.size);track.play()
   handler.postDelayed({try{track.stop()}catch(_:Exception){};track.release()},ms.toLong()+80)
  }catch(_:Exception){}
  handler.postDelayed({play(index+1,token)},durations[i].toLong()+90)
 }
}