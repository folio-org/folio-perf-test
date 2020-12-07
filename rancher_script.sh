#!/bin/bash
#TO-DO: Rancher log in

# rancher login https://rancher.dev.folio.org/v3 --token <BEARER_TOKEN> --context <CONTEXT>

# Function to get current template version
	get_template_version() {
		rancher app ls | grep $1 | tr -s " " | cut -d " " -f 6
	}

#Create arry
## Put in arry pair: mod version

#mapfile -t mas < <(jq -r ".[].id" modules.json | sed -E  "s/(.*)(-)([0-9].*$)/\1 \3/")
mapfile -t mas < <(cat modules | tr ' ' '\n' | sed -E  "s/(.*)(-)([0-9].*$)/\1 \3/")
#loop

for mod in "${mas[@]}"
do
  IFS=" " read -a arr <<< $mod

  rancher app upgrade --set image.tag=${arr[1]} ${arr[0]} $(get_template_version ${arr[0]})

#  echo "mod: ${arr[0]} version: ${arr[1]}"
done