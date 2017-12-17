if [ -d ace-builds ]; then
  rm -rf ace-builds
fi
if [ -d ace ]; then
  rm -rf ace
fi
git clone https://github.com/ajaxorg/ace.git
cd ace
npm install
nodejs Makefile.dryice.js full --target ../ace-builds
date >../ace-builds/build.info
git rev-parse HEAD >>../ace-builds/build.info
echo ==================================================================
echo Now you can copy ace-builds/ into public/ dir of aceeditor module.

