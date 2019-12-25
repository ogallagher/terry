_Owen Gallagher_<br>
_21 december 2019_

# Using deepspeech with a trained model and the osx command-line binding

Clone https://github.com/mozilla/DeepSpeech.git. You'll need to `brew install git-lfs` if you don't have it already; otherwise not all the files will appear in your local copy.

Then download the trained model from https://github.com/mozilla/DeepSpeech/releases/download/v0.6.0/deepspeech-0.6.0-models.tar.gz.

Before compiling deepspeech, install the following dependencies:

```
pip3 install six
```

Download the command-line binding for osx with `python3 util/taskcluster.py --arch osx --target .` The executable file `deepspeech` is now in the current directory.

Example use: `./deepspeech --model models/output_graph.pbmm --lm models/lm.binary --trie models/trie --stream 320 --beam_width 28 --audio audio/owen_transcribe_this.wav`. The `--stream` parameter allows the results to be generated continuously until the end of the audio file is reached. `beam_width` defines how much accuracy/speed tradeoff there is.

To use the `deepspeech` executable binary file, you just need to keep `models/{output_graph.pbmm,lm.binary,trie}` and the `deepspeechlib.so` file.
