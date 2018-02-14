package test.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class OctaneFindActionHandler extends AbstractHandler {

	CallHierarchy callHierarchy;
	Map<String, String> methodCache;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		MessageConsole myConsole = findConsole("");
		MessageConsoleStream log = myConsole.newMessageStream();

		IWorkbenchPage activePage = window.getActivePage();
		ITextEditor editor = (ITextEditor) activePage.getActiveEditor();
		IJavaElement jElement = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		callHierarchy = CallHierarchy.getDefault();
		StringBuffer caller = new StringBuffer();
		StringBuffer currentMethod = new StringBuffer();
		Map<String, String> methodCache = new HashMap<String, String>();
		if (jElement instanceof ICompilationUnit) {
			ITextSelection sel = (ITextSelection) editor.getSelectionProvider().getSelection();
			try {
				IJavaElement selected = ((ICompilationUnit) jElement).getElementAt(sel.getOffset());
				if (selected != null && selected.getElementType() == IJavaElement.METHOD) {
					IMethod method = (IMethod) selected;
					
					IType type = method.getDeclaringType();
 					MessageDialog.openInformation(window.getShell(), "Selected method ",
							"Method name " + method.getElementName());
					log.println("Method name " + method.getElementName());
					log.println("Type name "+type.getFullyQualifiedName());

					IMember[] members = { method };
					MethodWrapper[] callerMWrappers = callHierarchy.getCallerRoots(members);
					for (MethodWrapper mwrapper : callerMWrappers) {
						MethodWrapper[] callWrapper = mwrapper.getCalls(new NullProgressMonitor());
						for (MethodWrapper mw : callWrapper) {
							if (!mw.getName().equalsIgnoreCase("main")) {
								IMember member = mwrapper.getMember();
								IMember parentMember = mw.getParent().getMember();

								Collection<CallLocation> callLocations = mw.getMethodCall().getCallLocations();
								callLocations.forEach(new Consumer<CallLocation>() {
									
									public void accept(CallLocation location) {
										
										currentMethod.append(
												location.getCallText() + "\\" + type.getParent().getElementName());
										
										caller.append(mw.getName() + "\\"
												+ location.getMember().getParent().getParent().getElementName());
										
										System.out.println(currentMethod.toString() + " called by " + caller.toString());
										methodCache.put(currentMethod.toString(), caller.toString());
										currentMethod.setLength(0);
										caller.setLength(0);
										System.out.println(location.getMember().getParent());
										IMember[] member = {location.getMember()};
										System.out.println("Declaring type...."+location.getMember().getDeclaringType());
										IType decType = location.getMember().getDeclaringType();
										printMethodInfo(member, currentMethod, caller, decType);
										System.out.println("step 1..."+location.getMember());										
										ResolvedSourceMethod rsm = (ResolvedSourceMethod)location.getMember();
										System.out.println(rsm.getTypeRoot().getElementName());
										
//										String ss = location.getCallText() + "\\" + type.getParent().getElementName()
//												+ "\t called by ------------> " + mw.getName() + "\\"
//												+ location.getMember().getParent().getParent().getElementName()
//												+ "\n\n";
//										log.println(ss);
										
									};
								});
							}
						}
					}

				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private void printMethodInfo(IMember[] members, StringBuffer currentMethod, StringBuffer caller, IType decType) {
		
		MethodWrapper[] callerMWrappers = callHierarchy.getCallerRoots(members);
		for (MethodWrapper mwrapper : callerMWrappers) {
			MethodWrapper[] callWrapper = mwrapper.getCalls(new NullProgressMonitor());
			for (MethodWrapper mw : callWrapper) {
				System.out.println(mw.getName());
				if (!mw.getName().equalsIgnoreCase("main")) {
					IMember member = mwrapper.getMember();
					IMember parentMember = mw.getParent().getMember();

					Collection<CallLocation> callLocations = mw.getMethodCall().getCallLocations();
					callLocations.forEach(new Consumer<CallLocation>() {
						
						public void accept(CallLocation location) {
							
							currentMethod.append(
									location.getCallText() + "\\" + decType.getParent().getElementName());
							
							caller.append(mw.getName() + "\\"
									+ location.getMember().getParent().getParent().getElementName());
							
							System.out.println(currentMethod.toString() + " called by " + caller.toString());
							methodCache.put(currentMethod.toString(), caller.toString());
							currentMethod.setLength(0);
							caller.setLength(0);
							
							
							IMember[] member = {location.getMember()};								
							System.out.println("step 1..."+location.getMember());										
							ResolvedSourceMethod rsm = (ResolvedSourceMethod)location.getMember();
							System.out.println(rsm.getTypeRoot().getElementName());
							
//							String ss = location.getCallText() + "\\" + type.getParent().getElementName()
//									+ "\t called by ------------> " + mw.getName() + "\\"
//									+ location.getMember().getParent().getParent().getElementName()
//									+ "\n\n";
//							log.println(ss);
							
						};
					});
				}
			}
		}
	}
	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}
}
