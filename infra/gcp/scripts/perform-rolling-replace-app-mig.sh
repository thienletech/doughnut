#!/bin/bash
gcloud compute instance-groups managed rolling-action refresh doughnut-app-group --max-surge 1 --max-unavailable 0 --zone us-east1-b
