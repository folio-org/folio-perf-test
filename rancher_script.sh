#!/bin/sh
# Function to get current template version
	get_template_version() {
		rancher app ls | grep $1 | tr -s " " | cut -d " " -f 6
	}

#Create arry
## Put in arry pair: mod version
mas=()
while IFS= read -r line; do
  mas+=("$line")
done < modules

#mapfile -t mas < <(cat modules | tr ' ' '\n' | sed -E  "s/(.*)(-)([0-9].*$)/\1 \3/")
#loop

for mod in "${mas[@]}"
do
  IFS=" " read -a arr <<< $mod

  rancher app upgrade --set image.tag=${arr[1]} ${arr[0]} $(get_template_version ${arr[0]})

#  echo "mod: ${arr[0]} version: ${arr[1]}"
done