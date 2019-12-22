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

Example use: `./deepspeech --model models/output_graph.pbmm --lm models/lm.binary --trie models/trie --stream 320 --audio /Users/owengallagher/Documents/juniata/2019_senior/cs_seminar/deepspeech/audio/owen_not_accurate.wav`. The `--stream` parameter allows the results to be generated continuously until the end of the audio file is reached.

To use the `deepspeech` executable binary file, you just need to keep `models/` and the `deepspeechlib.so` file.
