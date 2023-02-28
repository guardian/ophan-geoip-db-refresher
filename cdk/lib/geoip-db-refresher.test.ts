import { ContextKeys } from '@guardian/cdk/lib/constants';
import { App } from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GeoipDbRefresher } from './geoip-db-refresher';

describe('The GeoipDbRefresher stack', () => {
	it('matches the snapshot', () => {
		const app = new App({
			context: {
				[ContextKeys.REPOSITORY_URL]: 'guardian/ophan-geoip-db-refresher',
			},
		});
		const stack = new GeoipDbRefresher(app, 'GeoipDbRefresher', {
			stack: 'ophan',
			stage: 'TEST',
		});
		const template = Template.fromStack(stack);
		expect(template.toJSON()).toMatchSnapshot();
	});
});
