import { GuScheduledLambda } from '@guardian/cdk';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import type { App } from 'aws-cdk-lib';
import { Duration } from 'aws-cdk-lib';
import { Schedule } from 'aws-cdk-lib/aws-events';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Architecture, Runtime } from 'aws-cdk-lib/aws-lambda';

export class GeoipDbRefresher extends GuStack {
	constructor(scope: App, id: string, props: GuStackProps) {
		super(scope, id, props);

		const app = 'geoip-db-refresher';

		const loggingPolicy = new PolicyStatement({
			resources: ['arn:aws:logs:*:*:*'],
			actions: [
				'logs:CreateLogGroup',
				'logs:CreateLogStream',
				'logs:PutLogEvents',
			],
		});

		const geoIpDbRefresherActionsPolicy = new PolicyStatement({
			resources: ['arn:aws:s3:::ophan-dist/geoip/*'],
			actions: ['s3:PutObject', 's3:PutObjectAcl'],
		});

		const ssmGetParameterPolicy = new PolicyStatement({
			resources: [
				`arn:aws:ssm:eu-west-1:${this.account}:parameter/Ophan/GeoIP`,
			],
			actions: ['ssm:GetParameter'],
		});

		const kmsDecryptPolicy = new PolicyStatement({
			resources: [
				`arn:aws:kms:eu-west-1:${this.account}:key/d77985cc-fb91-42e5-86f9-505fe2eefb76`,
			],
			actions: ['kms:Decrypt'],
		});

		const lambda = new GuScheduledLambda(this, 'geoip-db-refresher', {
			app,
			fileName: 'geoip-db-refresher.jar',
			description:
				'Fetching the latest GeoIP database and putting it in S3 for Ophan',
			handler: 'ophan.geoip.db.refresher.Lambda::handler',
			runtime: Runtime.JAVA_11,
			memorySize: 1536, // more memory than we need, but we're billed for fewer GB-seconds this way
			architecture: Architecture.ARM_64,
			timeout: Duration.seconds(120),
			// MaxMind: "Databases updated twice-weekly on Tuesdays and Fridays" ... can be "delayed by about one day"
			rules: [
				{ schedule: Schedule.expression('cron(20 11 ? * WED,THU,SAT,SUN *)') },
			],
			monitoringConfiguration: { noMonitoring: true },
		});

		lambda.addToRolePolicy(loggingPolicy);
		lambda.addToRolePolicy(geoIpDbRefresherActionsPolicy);
		lambda.addToRolePolicy(ssmGetParameterPolicy);
		lambda.addToRolePolicy(kmsDecryptPolicy);
	}
}
