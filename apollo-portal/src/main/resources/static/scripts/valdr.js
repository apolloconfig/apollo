app_module.config(appValdr);
setting_module.config(appValdr);

function appValdr(valdrProvider) {
    valdrProvider.addConstraints({
        'App': {
            'appId': {
                'size': {
                    'max': 64,
                    'message': 'Valdr.App.AppId.Size'
                },
                'required': {
                    'message': 'Valdr.App.AppId.Required'
                }
            },
            'appName': {
                'size': {
                    'max': 128,
                    'message': 'Valdr.App.appName.Size'
                },
                'required': {
                    'message': 'Valdr.App.appName.Required'
                }
            }
        }
    })
}

cluster_module.config(function (valdrProvider) {
    valdrProvider.addConstraints({
        'Cluster': {
            'clusterName': {
                'size': {
                    'max': 32,
                    'message': 'Valdr.Cluster.ClusterName.Size'
                },
                'required': {
                    'message': 'Valdr.Cluster.ClusterName.Required'
                }
            }
        }
    })
});

namespace_module.config(function (valdrProvider) {
    valdrProvider.addConstraints({
        'AppNamespace': {
            'namespaceName': {
                'size': {
                    'max': 32,
                    'message': 'Valdr.AppNamespace.NamespaceName.Size'
                },
                'required': {
                    'message': 'Valdr.AppNamespace.NamespaceName.Required'
                }
            },
            'comment': {
                'size': {
                    'max': 64,
                    'message': 'Valdr.AppNamespace.Comment.Size'
                }
            }
        }
    })
});

application_module.config(function (valdrProvider) {
    valdrProvider.addConstraints({
        'Item': {
            'key': {
                'size': {
                    'max': 128,
                    'message': 'Valdr.Item.Key.Size'
                },
                'required': {
                    'message': 'Valdr.Item.Key.Required'
                }
            },
            'comment': {
                'size': {
                    'max': 64,
                    'message': 'Valdr.Item.Comment.Size'
                }
            }
        },
        'Release': {
            'releaseName': {
                'size': {
                    'max': 64,
                    'message': 'Valdr.Release.ReleaseName.Size'
                },
                'required': {
                    'message': 'Valdr.Release.ReleaseName.Size'
                }
            },
            'comment': {
                'size': {
                    'max': 64,
                    'message': 'Valdr.Release.Comment.Size'
                }
            }
        }
    })
});


