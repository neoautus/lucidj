if [ -d ace-builds ]; then
  rm -rf ace-builds
fi
if [ -d ace ]; then
  rm -rf ace
fi
git clone https://github.com/ajaxorg/ace.git
BASE=`pwd`
cd ace
npm install
nodejs Makefile.dryice.js full --target ../ace-builds
date >../ace-builds/build.info
git rev-parse HEAD >>../ace-builds/build.info
cd "$BASE"
zip -r ace-builds.zip ace-builds
echo ==================================================================
echo Now you can commit the new ace-builds.zip with build.info:
cat ace-builds/build.info
