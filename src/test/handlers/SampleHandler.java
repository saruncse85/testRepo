package test.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPage activePage = window.getActivePage();
		ITextEditor editor = (ITextEditor) activePage.getActiveEditor();
		IJavaElement jElement = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		if(jElement instanceof ICompilationUnit ) {
			ITextSelection sel = (ITextSelection) editor.getSelectionProvider().getSelection();
			try {
				IJavaElement selected = ((ICompilationUnit)jElement).getElementAt(sel.getOffset());
				if(selected != null && selected.getElementType() == IJavaElement.METHOD) {
					IMethod method = (IMethod)selected;
					MessageDialog.openInformation(window.getShell(), "Selected method ", "Method name "+method.getElementName());
					System.out.println("Method name "+method.getElementName());
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		
		MessageDialog.openInformation(window.getShell(), "Test", "Start analyzing methods in com.rens.ubs.common.broker.sybase.CalcBroker.java ");
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			File fis= new File("T:\\output.txt");
			IProject project = root.getProject("2018-6.4.1");
			FileOutputStream fos = new FileOutputStream(fis);
			
			IJavaProject javaProject = JavaCore.create(project);
			IPackageFragment[] packFrag = javaProject.getPackageFragments();
			CallHierarchy callHierarchy = CallHierarchy.getDefault();
			Map<String, String> methodCache = new HashMap<String, String>();
			StringBuffer caller = new StringBuffer();
			StringBuffer currentMethod = new StringBuffer();
			
			for (IPackageFragment frag : packFrag) {
				if(frag.getElementName().equalsIgnoreCase("com.rens.ubs.common.broker.sybase")) {
					ICompilationUnit[] units = frag.getCompilationUnits();
					for (ICompilationUnit unit : units) {
						IType[] types = unit.getTypes();
						for (IType type : types) {
							if(type.getElementName().startsWith("CalcBroker")) {
								IMethod[] methods = type.getMethods();
								for (IMethod method : methods) {
									// final List<String> referenceList = new ArrayList<String>();
									// String methodName = method.getElementName();
									// if (!method.isConstructor()) {
									// // Finds the references of the method and record references of the method to
									// referenceList parameter.
									// JDTSearchProvider.searchMethodReference(referenceList, method, scope,
									// iJavaProject);
									// }
									IMember[] members = { method };
//									System.out.println("********************Caller roots*****************");
									MethodWrapper[] callerMWrappers = callHierarchy.getCallerRoots(members);
									//System.out.println(callerMWrappers);
									for(MethodWrapper mwrapper : callerMWrappers) {
										//System.out.println("Level "+mwrapper.getLevel());
										//System.out.println(".............getCalls ...."+mwrapper.getCalls(new NullProgressMonitor()));
										MethodWrapper[] callWrapper = mwrapper.getCalls(new NullProgressMonitor());
										for(MethodWrapper mw : callWrapper) {
											if(!mw.getName().equalsIgnoreCase("main")) {
												IMember member = mwrapper.getMember();										
												IMember parentMember = mw.getParent().getMember();
												
												Collection<CallLocation> callLocations = mw.getMethodCall().getCallLocations();
												callLocations.forEach(new Consumer<CallLocation>() {
													public void accept(CallLocation location) {
														currentMethod.append(location.getCallText()+"\\"+type.getParent().getElementName());
														caller.append(mw.getName()+"\\"+location.getMember().getParent().getParent().getElementName());
														System.out.println(currentMethod.toString() +" called by "+caller.toString());
														methodCache.put(currentMethod.toString(), caller.toString());
														currentMethod.setLength(0);
														caller.setLength(0);
//														System.out.println("++++++++++++++call text "+location.getCallText()+" called by ///////////////////////"+mw.getName());
//														System.out.println("+++++++++++ call type "+location.getMember().getParent().getParent().getElementName());
														//MessageDialog.openInformation(window.getShell(), "Test", location.getCallText()+"\\"+type.getParent().getElementName()+" called by ------------ "+mw.getName()+"\\"+location.getMember().getParent().getParent().getElementName());
														String ss = location.getCallText()+"\\"+type.getParent().getElementName()+"\t called by ------------> "+mw.getName()+"\\"+location.getMember().getParent().getParent().getElementName()+"\n\n";
														try {
															fos.write(ss.getBytes());
															
														} catch(Exception ex) {
															ex.printStackTrace();
														}
													};
												});
												System.out.println("****************************************");
											}
										}
										
//										if(mwrapper.getMethodCall() != null) {
//											System.out.println("First call location "+mwrapper.getMethodCall().getFirstCallLocation());
//											System.out.println("All call locations "+mwrapper.getMethodCall().getCallLocations());
//										}
//										System.out.println("Parent "+mwrapper.getParent());
//										if(mwrapper.getParent() != null) {
//											System.out.println("Parent name"+mwrapper.getParent().getName());
//											System.out.println("Parent class name "+mwrapper.getParent().getClass());
//										}
									}
									
//									System.out.println("********************Callee roots*****************");
//									MethodWrapper[] calleeMWrapper = callHierarchy.getCallerRoots(members);
//									System.out.println(calleeMWrapper);
//									for(MethodWrapper mwrapper : calleeMWrapper) {
//										System.out.println("name "+mwrapper.getName());
//										System.out.println("Level "+mwrapper.getLevel());
//										System.out.println(".............getCalls ...."+mwrapper.getCalls(new NullProgressMonitor()));
//										System.out.println("method call "+mwrapper.getMethodCall());
//										if(mwrapper.getMethodCall() != null) {
//											System.out.println("First call location "+mwrapper.getMethodCall().getFirstCallLocation());
//											System.out.println("All call locations "+mwrapper.getMethodCall().getCallLocations());
//										}
//										
//										System.out.println("Parent "+mwrapper.getParent());
//										if(mwrapper.getParent() != null) {
//											System.out.println("Parent name"+mwrapper.getParent().getName());
//											System.out.println("Parent class name "+mwrapper.getParent().getClass());
//										}
//									}	
//									MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
//									HashSet<IMethod> callers = new HashSet<IMethod>();
//									for (MethodWrapper mw : methodWrappers) {
//										MethodWrapper[] mw2 = mw.getCalls(new NullProgressMonitor());
//										HashSet<IMethod> temp = getIMethods(mw2);
//										callers.addAll(temp);
//									}
								}
							}
						}
					}
					// for(IJavaElement jElement : javaElements) {
					// System.out.println("Element Name "+jElement.getElementName());
					// System.out.println("Element Type "+jElement.getElementType());
					// System.out.println("parent element name
					// "+jElement.getParent().getElementName());
					// }
				}
				
			}
//			SearchPattern pattern = SearchPattern.createPattern("display()", IJavaSearchConstants.METHOD,
//					IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);
//
//			// IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
//
//			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(packFrag);
//
//			SearchRequestor requestor = new SearchRequestor() {
//
//				@Override
//				public void acceptSearchMatch(SearchMatch match) throws CoreException {
//					IMethod element = (IMethod) match.getElement();
//					element.getElementName();
//
//					CallLocation location = CallHierarchy.getCallLocation(element);
//					System.out.println(location.getCalledMember().getElementName());
//					System.out.println(location.getMember().getElementName());
//
//					// callHierachy.getCallerRoots(members)
//				}
//			};
//
//			SearchEngine searchEngine = new SearchEngine();
//			try {
//				searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
//						scope, requestor, null);
//			} catch (CoreException e) {
//				e.printStackTrace();
//			}
			try {
				fos.flush();
			} catch(Exception ex) {
				ex.printStackTrace();
			} finally {
				fos.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("completed");
		MessageDialog.openInformation(window.getShell(), "Test", "Completed !!! Please check T:\\output.txt file ");
		return null;
	}

	HashSet<IMethod> getIMethods(MethodWrapper[] methodWrappers) {
		HashSet<IMethod> c = new HashSet<IMethod>();
		for (MethodWrapper m : methodWrappers) {
			IMethod im = getIMethodFromMethodWrapper(m);
			if (im != null) {
				c.add(im);
			}
		}
		return c;
	}

	IMethod getIMethodFromMethodWrapper(MethodWrapper m) {
		try {
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD) {
				return (IMethod) m.getMember();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
