#!/usr/bin/env bash

set -e

if [[ -z "$1" || -z "$2" ]]
  then
    echo "Missing mandatory arguments: path_shared_library, path_where_to_copy"
    exit 1
fi

path_shared_library=$1
copy_path=$2
path_to_json="/tmp/topo_sorted.json"

RUST_LOG=info /opt/lddtopo-rs/target/release/lddtopo-rs --shared-library-path $path_shared_library --output-file $path_to_json

# Skipping libmfhdfalt due to /tmp/xxxx.7669203887046987097/libmfhdfalt_5076252650710112250.so: undefined symbol: error_top
# Skip libproj.so.15
modules_to_ignore=("ld-linux-x86-64" "libc.so" "libm.so" "libpthread"  "libstdc" "libmfhdfalt" "libproj.so.15")
os_name="linux"
arch_type="x86-64"

dest_folder="$copy_path/native/${os_name}-${arch_type}"
mkdir -p $dest_folder

path_native_modules_to_load="${copy_path}/native/${os_name}-${arch_type}.txt"
rm -rf "$path_native_modules_to_load"

for OUTPUT in $(jq -r '.topo_sorted_libs[] |  .name + ":" + .path' $path_to_json)
do
  lib=$(echo $OUTPUT | cut -d ":" -f 1)
  found=false
  for i in "${modules_to_ignore[@]}"; do
      if grep -q "$i" <<< "$lib"; then
          #echo "Found $i in $lib, ignoring...";
          found=true;
          break;
      fi
  done

  if $found; then
    continue;
  fi

  path=$(echo $OUTPUT | cut -d ":" -f 2)
  dest_path="${dest_folder}/$lib"
  #echo $dest_path

  cp $path $dest_path
  echo "Copied $lib to $dest_path"

  # Write native module to the list of modules
  echo $lib >> $path_native_modules_to_load
done

