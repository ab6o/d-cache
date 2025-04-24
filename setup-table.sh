#!/bin/bash
set -e

# Default values
TABLE_NAME="http-cache"
REGION="us-east-1"
READ_CAPACITY=5
WRITE_CAPACITY=5
TTL_ATTRIBUTE="expires"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --table-name)
      TABLE_NAME="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    --read-capacity)
      READ_CAPACITY="$2"
      shift 2
      ;;
    --write-capacity)
      WRITE_CAPACITY="$2"
      shift 2
      ;;
    --ttl-attribute)
      TTL_ATTRIBUTE="$2"
      shift 2
      ;;
    --delete)
      DELETE=true
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Options:"
      echo "  --table-name NAME      DynamoDB table name (default: http-cache)"
      echo "  --region REGION        AWS region (default: us-east-1)"
      echo "  --read-capacity N      Read capacity units (default: 5)"
      echo "  --write-capacity N     Write capacity units (default: 5)"
      echo "  --ttl-attribute NAME   TTL attribute name (default: expires)"
      echo "  --delete               Delete the table instead of creating it"
      echo "  --help                 Display this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
  echo "Error: AWS CLI is not installed. Please install it first."
  exit 1
fi

# Check if the table exists
TABLE_EXISTS=$(aws dynamodb describe-table --table-name $TABLE_NAME --region $REGION 2>/dev/null || echo "not_found")

if [ "$DELETE" = true ]; then
  # Delete the table if requested
  if [[ $TABLE_EXISTS != "not_found" ]]; then
    echo "Deleting DynamoDB table: $TABLE_NAME"
    aws dynamodb delete-table --table-name $TABLE_NAME --region $REGION
    echo "Waiting for table deletion to complete..."
    aws dynamodb wait table-not-exists --table-name $TABLE_NAME --region $REGION
    echo "Table $TABLE_NAME has been deleted."
  else
    echo "Table $TABLE_NAME does not exist."
  fi
else
  # Create the table if it doesn't exist
  if [[ $TABLE_EXISTS == "not_found" ]]; then
    echo "Creating DynamoDB table: $TABLE_NAME"
    aws dynamodb create-table \
      --table-name $TABLE_NAME \
      --attribute-definitions AttributeName=key,AttributeType=S \
      --key-schema AttributeName=key,KeyType=HASH \
      --provisioned-throughput ReadCapacityUnits=$READ_CAPACITY,WriteCapacityUnits=$WRITE_CAPACITY \
      --region $REGION
    
    echo "Waiting for table creation to complete..."
    aws dynamodb wait table-exists --table-name $TABLE_NAME --region $REGION
    
    echo "Enabling TTL on attribute: $TTL_ATTRIBUTE"
    aws dynamodb update-time-to-live \
      --table-name $TABLE_NAME \
      --time-to-live-specification "Enabled=true,AttributeName=$TTL_ATTRIBUTE" \
      --region $REGION
    
    echo "Table $TABLE_NAME has been created with TTL enabled."
  else
    echo "Table $TABLE_NAME already exists."
  fi
fi 