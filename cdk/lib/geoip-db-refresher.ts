import { GuScheduledLambda } from '@guardian/cdk';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import type { App } from 'aws-cdk-lib';
import { Schedule } from 'aws-cdk-lib/aws-events';
import { Architecture, Runtime } from 'aws-cdk-lib/aws-lambda';

export class GeoipDbRefresher extends GuStack {
	constructor(scope: App, id: string, props: GuStackProps) {
		super(scope, id, props);

		const app = 'geoip-db-refresher';

		const lambda = new GuScheduledLambda(this, 'geoip-db-refresher', {
			app,
			fileName: 'geoip-db-refresher.jar',
			description:
				'Fetching the latest GeoIP database and putting it in S3 for Ophan',
			handler: 'ophan.geoip.db.refresher.Lambda::handler',
			runtime: Runtime.JAVA_11,
			architecture: Architecture.ARM_64,
			// MaxMind: "Databases updated twice-weekly on Tuesdays and Fridays" ... can be "delayed by about one day"
			rules: [
				{ schedule: Schedule.expression('cron(20 11 ? * WED,THU,SAT,SUN *)') },
			],
			monitoringConfiguration: { noMonitoring: true },
		});
	}
}
