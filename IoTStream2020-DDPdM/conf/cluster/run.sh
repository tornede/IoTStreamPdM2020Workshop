#!/bin/bash
#CCS -N lossGuidance
#CCS --res=rset=1:ncpus=8:mem=30g:vmem=30g
#CCS -t 15h
#CCS -M tornede@mail.uni-paderborn.de
#CCS -meas
#CCS -J 0-99:1

module add singularity

cd /upb/scratch/departments/pc2/groups/hpc-prf-isys/tornede/experiments/experimentArray/

singularity exec mlplan_pdm.sif bash -c "./run_in_container.sh"
