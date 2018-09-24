#!/bin/bash -x

if [[ $# -lt 4 ]]; then
   echo "Usage: $0 <STARTUP_THREADS> <RAMP-UP> <ITERATIONS> <ENDING_RPS>"
   echo "Eg: $0 50 10 1 500"
   echo "STARTUP_THREADS: Number of threads(users)"
   echo "RAMPUP: ram-Up period (in seconds)"
   echo "ITERATIONS: Loop count"
   echo "MAX_RPS: Maximum threashold for Requests per second"
   exit 1;
fi

STARTUP_THREADS=${1}
RAMPUP=${2}
ITERATIONS=${3}
ENDING_RPS=${4}

let totalThreads=${STARTUP_THREADS}
let rampupTime=${RAMPUP}
let iterations=${ITERATIONS}
let startingRPS=$((totalThreads/rampupTime))     #calculate starting request per second

&> capacity.csv                              #clean-up previous content before writing new 

while (( ${startingRPS} < ENDING_RPS )); do
    if (( ${startingRPS} <= 0 || ${totalThreads} <= 0 || ${rampupTime} <= 0 )); then
        echo "Error: Starting request per second or threads or ramp-up time should not be less or equal to 0"
        break;
    fi
    
    startingRPS=$((totalThreads/rampupTime))
    
    echo "$startingRPS,$totalThreads,$rampupTime,$iterations"  >> capacity.csv   #Write to csv file
    totalThreads=$((totalThreads+STARTUP_THREADS))  #Ramp-up threads
done
