// for testing only, not included in plugin zip
grails {
	plugins {
		springsecurity {
			userLookup {
				userDomainClassName = 'test.TestUser'
				usernamePropertyName = 'loginName'
				enabledPropertyName = 'enabld'
				passwordPropertyName = 'passwrrd'
				authoritiesPropertyName = 'roles'
			}

			requestMap {
				className = 'test.TestRequestmap'
				urlField = 'urlPattern'
				configAttributeField = 'rolePattern'
			}

			authority {
				className = 'test.TestRole'
				nameField = 'auth'
			}
		}
	}
}
