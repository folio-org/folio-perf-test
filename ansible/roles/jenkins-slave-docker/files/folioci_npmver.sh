#!/bin/bash 

# for testing only
#BUILD_NUMBER=123

if [ ! -e package.json ]; then 
   echo "package.json file not found"
   exit 2
fi

python=`which python`

if [ -z "$python" ]; then
  echo "python not found"
  exit 2
else 
  export PYTHONIOENCODING=utf8
fi

cur_ver=`cat package.json | 
         python -c "import sys, json; print json.load(sys.stdin)['version']"`

maj_ver=$(echo $cur_ver | awk -F '.' '{ print $1 }')
min_ver=$(echo $cur_ver | awk -F '.' '{ print $2 }')
patch_ver=$(echo $cur_ver | awk -F '.' '{ print $3 }')

if [ "$patch_ver" == "0" ]; then
  patch_ver=1
fi

new_cur_ver=${maj_ver}.${min_ver}.${patch_ver}

# add 00+Jenkins BUILD_NUMBER to current patch version

new_snap_ver=${new_cur_ver}00${BUILD_NUMBER}
echo "$new_snap_ver"

