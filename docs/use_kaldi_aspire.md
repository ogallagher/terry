_Owen Gallagher_<br>
_21 December 2019_

# Using kaldi with the aspire trained model

Before using anything in kaldi you need to build it. I didn't do this step at first, so I needed to go to `kaldi/cmake/` and follow the instructions there.

Download the trained model with `curl https://kaldi-asr.org/models/1/0001_aspire_chain_model.tar.gz > 0001_aspire_chain_model.tar.gz` and then extract it and toss the compressed tar file. The contents should not be in a separate directory from the rest of `s5/`.

The `conf/mfcc_hires.conf` file should already have the correct values, but if not, modify it to have:

```

--use-energy=false
--sample-frequency=8000
--num-mel-bins=40
--num-ceps=40   
--low-freq=40 
--high-freq=-200

```

Using that .conf file, run `steps/online/nnet3/prepare_online_decoding.sh \
  --mfcc-config conf/mfcc_hires.conf data/lang_chain exp/nnet3/extractor exp/chain/tdnn_7b exp/tdnn_7b_chain_online` to create the `final.mdl` model file that generates ivectors from the utterances to be passed to the neural network on a run.

Next, create the graph fst `HCLG.fst` from the grammar `G.fst` and lexicon `L.fst` with this command: `utils/mkgraph.sh --self-loop-scale 1.0 data/lang_pp_test exp/tdnn_7b_chain_online exp/tdnn_7b_chain_online/graph_pp`. The final fst files are at `exp/chain/tdnn_7b` and `exp/tdnn_7b_chain_online`. 

`brew install ffmpeg` This is a multimedia tool for handling video and audio.

`brew install sox` This is an audio tool needed to change the frequency of the .wav files.

Create an example audio file in quicktime player at `audio/owen_prince_is_a_dog.m4a`, which can then be converted to a mono-channel wav file using `ffmpeg -i owen_prince_is_a_dog.m4a -ac 1 owen_prince_is_a_dog.wav`. This will later be turned into an utterance with the correct samplerate through sox.

Create a wav.scp file, which associates utterances with source paths. The contents are:

```
owen_prince_is_a_dog `which sox` -t wav /Users/owengallagher/Documents/juniata/cs_research/kaldi/egs/aspire/s5/audio/owen_prince_is_a_dog.wav -c 1 -b 16 -r 8000 -t wav - |

```

Next is the utt2spk file, which assigns these input utterances to speakers.

```

owen_prince_is_a_dog owen

```