package com.whispercpp.java.whisper;

public class WhisperSegment {
  public final long start;
  public final long end;
  public final String sentence;

  public WhisperSegment(long start, long end, String sentence) {
    this.start = start;
    this.end = end;
    this.sentence = sentence;
  }

  @Override
  public String toString() {
    return "[" + start + " - " + end + "]: " + sentence;
  }
}
