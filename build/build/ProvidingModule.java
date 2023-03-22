package build;

sealed interface ProvidingModule permits VendorModule, SourceModule {

	String name();
}
