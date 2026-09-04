from faster_whisper import WhisperModel

_model = None


def get_model():
    global _model

    if _model is None:
        print("Loading Whisper model...")
        _model = WhisperModel(
            "base",
            device="cpu",
            compute_type="int8",
        )
        print("Whisper model loaded.")

    return _model


def transcribe_audio(audio_path: str):
    model = get_model()

    segments, info = model.transcribe(
        audio_path,
        beam_size=5,
        vad_filter=True,
    )

    text = " ".join(
        segment.text.strip()
        for segment in segments
        if segment.text.strip()
    ).strip()

    return {
        "text": text,
        "language": info.language,
        "languageProbability": info.language_probability,
    }