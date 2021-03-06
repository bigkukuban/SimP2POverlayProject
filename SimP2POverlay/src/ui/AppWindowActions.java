package ui;
import commonHelper.math.EuclideanPoint;
import launcher.ApplicationModelSettings;
import launcher.ApplicationModelSettings.SupportedTopologyTypes;
import networkInitializer.InitializerFactory;
import networkInitializer.NetworkSettingsBase;
import networkInitializer.baPreferentialAttachment.NetworkSettingsBaPreferentialAttachment;
import networkInitializer.chord.NetworkSettingsChord;
import networkInitializer.gridStructured.NetworkSettingsGrid;
import networkInitializer.interfaces.INetworkInitializer;
import networkInitializer.smallWorldKleinberg.NetworkSettingsSmallWorldKleinberg;
import peersModel.interfaces.INetworkFacade;
import persistence.NetworkToFilePersister;
import ui.openGL.ViewModelNetworkEvent.EventAssignNewNetwork;
import ui.openGL.ViewModelNetworkEvent.EventChangeCameraPosition;
import ui.openGL.ViewModelNetworkEvent.EventChangeZoomFactor;
import ui.openGL.ViewModelNetworkEvent.EventSetLookAtMouseClick;
import ui.openGL.interfaces.IPoint3D;

public class AppWindowActions 
{
		
	public static ApplicationModelSettings CreateInitialModelSettings()
	{
		ApplicationModelSettings result = new ApplicationModelSettings();
		
		result.AllGraphSettings.add(new NetworkSettingsGrid(10,10));
		result.AllGraphSettings.add(new NetworkSettingsSmallWorldKleinberg(10,10,1,1,1.0));	
		result.AllGraphSettings.add(new NetworkSettingsBaPreferentialAttachment(2,2,100));
		result.AllGraphSettings.add(new NetworkSettingsChord(5,10,true));
		
		result.ActiveSettings = result.AllGraphSettings.stream().
									  filter(s -> s instanceof NetworkSettingsBaPreferentialAttachment).
									  findFirst().get();

		result.NetworkFacade = CreateNetwork(result.ActiveSettings);
		
		return result;
	}
	
	public static INetworkFacade CreateNetwork(NetworkSettingsBase settings)
	{
		INetworkFacade result = null;				
		try {
			INetworkInitializer peerBoxInitializer = InitializerFactory.GetInitializerBySettingsType(settings, true);
			result = peerBoxInitializer.GetInitializedNetwork();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		return result;
	}
	
	
	public OpenGLView _openGlView;
	public ApplicationModelSettings ApplicationSettings = null;
	public ApplicationWindow _applicationWindow;
	public AppWindowActions()
	{		
		ApplicationSettings = CreateInitialModelSettings();
	}
	
	public void UserChangedCameraPositionByMouseClick(int mouseXPosition, int mouseYPosition)
	{
		_openGlView._networkViewModel.PlaceNewEventDelegate(new EventSetLookAtMouseClick(mouseXPosition,mouseYPosition));				
		_openGlView.UpdateCanvas();	
		IPoint3D cameraPos = _openGlView._networkViewModel.GetCameraPosition();
		_applicationWindow.UpdateUISettings(cameraPos.GetPosX(), cameraPos.GetPosY(), cameraPos.GetPosZ(), _openGlView._networkViewModel.GetZoomAngle());
	}
	
	public void UserChangedZoomValue(int zoomValue)
	{		
		_openGlView._networkViewModel.PlaceNewEventDelegate(new EventChangeZoomFactor(zoomValue));	  
		_openGlView.UpdateCanvas();
	}
	
	public void UsedResetedTheView()
	{
		_openGlView._networkViewModel.Reset();
		_openGlView.UpdateCanvas();			
		IPoint3D cameraPos = _openGlView._networkViewModel.GetCameraPosition();
		_applicationWindow.UpdateUISettings(cameraPos.GetPosX(), cameraPos.GetPosY(), cameraPos.GetPosZ(), _openGlView._networkViewModel.GetZoomAngle());
	}
		
	public void UserChangedCameraPosInApplilcationView(double dxPos, double dyPos, double dzPos)
	{
		_openGlView._networkViewModel.PlaceNewEventDelegate(new EventChangeCameraPosition(new EuclideanPoint(new double[]{dxPos,dyPos,dzPos})));	  
		_openGlView.UpdateCanvas();		
	}
	
	public void UserChangedSettingsForSmallWorld(int xItems, int yItems, int qParam, int pParam, double rParam)
	{				
		NetworkSettingsSmallWorldKleinberg settings = (NetworkSettingsSmallWorldKleinberg)ApplicationSettings.GetSettingsByType(SupportedTopologyTypes.SmallWorld);
		settings._xLength = xItems;
		settings._yLength = yItems;
		settings._qParameter = qParam;
		settings._pPParameter = pParam;
		settings._rParameter = rParam;
		
		if(ApplicationSettings.ActiveSettings == settings)
		{
			ApplicationSettings.NetworkFacade = CreateNetwork(settings);
			_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));	
			_openGlView.UpdateCanvas();
		}		
	}
	
	public void UserChangedSettingsForPreferentialAttachment(int m0, int m, int n)
	{
		if(m > m0 || n < m0) return; 
		
		
		NetworkSettingsBaPreferentialAttachment settings = (NetworkSettingsBaPreferentialAttachment)ApplicationSettings.GetSettingsByType(SupportedTopologyTypes.PreferentialAttachment);
		settings.m = m;
		settings.m0 = m0;
		settings.N = n;
				
		if(ApplicationSettings.ActiveSettings == settings)
		{
			ApplicationSettings.NetworkFacade = CreateNetwork(settings);
			_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));
			_openGlView.UpdateCanvas();
		}		
	}
	
	public void UserChangedSettingsForGrid(int numXItems, int numYItems)
	{
		NetworkSettingsGrid settings = (NetworkSettingsGrid)ApplicationSettings.GetSettingsByType(SupportedTopologyTypes.Grid);
		
		settings.XLength = numXItems;
		settings.YLength = numYItems;
		
		if(ApplicationSettings.ActiveSettings == settings)
		{
			ApplicationSettings.NetworkFacade = CreateNetwork(settings);
			_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));	
			_openGlView.UpdateCanvas();
		}		
	}
	
	public void UserChangedToOtherTopology(SupportedTopologyTypes newTopology)
	{
		//set new topology as active one, generate the network and update the ui
		
		NetworkSettingsBase settings = ApplicationSettings.GetSettingsByType(newTopology);
		ApplicationSettings.ActiveSettings = settings;
		
		ApplicationSettings.NetworkFacade = CreateNetwork(ApplicationSettings.ActiveSettings);
		_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));
		_openGlView.UpdateCanvas();
		
	}
	
	public boolean UserStoresTheSettings(String targetPath)
	{
		return NetworkToFilePersister.DoPersistNetwork(ApplicationSettings, targetPath);				
	}
	
	public boolean UserOpensSettingsFromFile(String sourcePath)
	{
		ApplicationModelSettings  newSettings = NetworkToFilePersister.DoRestoreNetwork(sourcePath);
		if(newSettings == null) return false;
		
		ApplicationSettings = newSettings;
		_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));
		_openGlView.UpdateCanvas();
				
		return true;
	}

	public void UserChangedSettingsForChord(int mValue, int nValue, boolean useRandomPlacing) 
	{
		NetworkSettingsChord settings = (NetworkSettingsChord)ApplicationSettings.GetSettingsByType(SupportedTopologyTypes.Chord);
		
		//some checks ... 		
		if(Math.pow(2, mValue)< nValue) return;
		
		settings._m = mValue;
		settings._N = nValue;
		settings._UseRandomNodePlacingInRing = useRandomPlacing;
		
		if(ApplicationSettings.ActiveSettings == settings)
		{
			ApplicationSettings.NetworkFacade = CreateNetwork(settings);
			_openGlView._networkViewModel.PlaceNewEventDelegate(new EventAssignNewNetwork(ApplicationSettings.NetworkFacade));	
			_openGlView.UpdateCanvas();
		}		
		
	}
}
